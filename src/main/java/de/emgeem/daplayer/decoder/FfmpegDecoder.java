package de.emgeem.daplayer.decoder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioFormat;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;

import de.emgeem.daplayer.model.Track;

/**
 * Universeller Decoder über FFmpeg (JavaCPP).
 * Formate: WMA, FLAC, OGG/Vorbis, Opus, M4A/AAC, AIFF, APE, WavPack sowie Netzwerk-Streams (http/https/…).
 * Beliebige Sample-Formate werden per libswresample nach 16 Bit interleaved konvertiert,
 * Mehrkanal-Material wird auf Stereo heruntergemischt.
 */
public class FfmpegDecoder implements AudioDecoder {

    private static final Set<String> EXTENSIONS = Set.of(
        "wma", "flac", "ogg", "oga", "opus", "m4a", "aac", "aiff", "aif", "ape", "wv"
    );
    private static volatile boolean networkInitialized;

    private String location;
    private boolean stream;
    private AudioFormat format;
    private AVFormatContext formatContext;
    private AVCodecContext codecContext;
    private AVPacket packet;
    private AVFrame frame;
    private SwrContext swrContext;
    private AVChannelLayout outLayout;
    private int audioStreamIndex = -1;
    private int outChannels;
    private int outSampleRate;
    private boolean inputEof;
    private boolean decoderEof;

    // aktuelle Konfiguration des Resamplers
    private int swrInFormat = -1;
    private int swrInRate = -1;
    private int swrInChannels = -1;

    private BytePointer outBuffer;
    private PointerPointer<BytePointer> outPointer;
    private int outBufferSamples;

    private byte[] pending = new byte[0];
    private int pendingOffset;
    private long skipUntilMillis = -1;

    @Override
    public boolean supports(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean supportsStreams() {return true;}

    @Override
    public Track readTrack(File file) {
        Track track = new Track(file);
        boolean tagsRead = TagReader.read(file, track);
        if (!tagsRead || track.getDuration() <= 0 || track.getSampleRate() <= 0) {
            probe(file, track);
        }
        if (track.getTitle() == null || track.getTitle().isBlank()) {track.setTitle(track.getFileName());}
        return track;
    }

    /** Liest Dauer/Format/Tags direkt über FFmpeg (Fallback, z. B. für Opus, APE, WV). */
    private void probe(File file, Track track) {
        AVFormatContext ctx = avformat.avformat_alloc_context();
        try {
            if (avformat.avformat_open_input(ctx, file.getAbsolutePath(), null, null) < 0) {return;}
            if (avformat.avformat_find_stream_info(ctx, (PointerPointer) null) < 0) {return;}
            if (track.getDuration() <= 0 && ctx.duration() > 0) {
                track.setDuration(ctx.duration() / 1000L); // AV_TIME_BASE = Mikrosekunden
            }
            for (int i = 0; i < ctx.nb_streams(); i++) {
                AVStream s = ctx.streams(i);
                if (s.codecpar().codec_type() != avutil.AVMEDIA_TYPE_AUDIO) {continue;}
                if (track.getSampleRate() <= 0) {track.setSampleRate(s.codecpar().sample_rate());}
                if (track.getChannels() <= 0) {track.setChannels(s.codecpar().ch_layout().nb_channels());}
                if (track.getBitDepth() <= 0) {track.setBitDepth(s.codecpar().bits_per_raw_sample());}
                if (track.getBitRate() <= 0 && s.codecpar().bit_rate() > 0) {track.setBitRate((int) (s.codecpar().bit_rate() / 1000));}
                fillFromDictionary(s.metadata(), track);
                break;
            }
            fillFromDictionary(ctx.metadata(), track);
        } catch (Throwable t) {
            System.err.println("FFmpeg-Probe fehlgeschlagen: " + file.getName() + " (" + t.getMessage() + ")");
        } finally {
            avformat.avformat_close_input(ctx);
        }
    }

    private static void fillFromDictionary(AVDictionary dict, Track track) {
        if (dict == null || dict.isNull()) {return;}
        boolean noTitle = track.getTitle() == null || track.getTitle().isBlank() || track.getTitle().equals(track.getFileName());
        if (noTitle) {track.setTitle(dictValue(dict, "title"));}
        if (track.getArtist() == null) {track.setArtist(dictValue(dict, "artist"));}
        if (track.getAlbum() == null) {track.setAlbum(dictValue(dict, "album"));}
        if (track.getAlbumArtist() == null) {track.setAlbumArtist(dictValue(dict, "album_artist"));}
        if (track.getGenre() == null) {track.setGenre(dictValue(dict, "genre"));}
        if (track.getYear() <= 0) {track.setYear(TagReader.parseLeadingInt(dictValue(dict, "date")));}
        if (track.getTrackNumber() <= 0) {track.setTrackNumber(TagReader.parseLeadingInt(dictValue(dict, "track")));}
        if (track.getDiscNumber() <= 0) {track.setDiscNumber(TagReader.parseLeadingInt(dictValue(dict, "disc")));}
    }

    private static String dictValue(AVDictionary dict, String key) {
        AVDictionaryEntry entry = avutil.av_dict_get(dict, key, (AVDictionaryEntry) null, avutil.AV_DICT_IGNORE_SUFFIX);
        if (entry == null || entry.isNull() || entry.value() == null) {return null;}
        String value = entry.value().getString(StandardCharsets.UTF_8);
        return value == null || value.isBlank() ? null : value.trim();
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    @Override
    public void open(File file) throws Exception {
        openInternal(file.getAbsolutePath(), false);
    }

    @Override
    public void openStream(String url) throws Exception {
        if (!networkInitialized) {
            synchronized (FfmpegDecoder.class) {
                if (!networkInitialized) {
                    avformat.avformat_network_init();
                    networkInitialized = true;
                }
            }
        }
        openInternal(url, true);
    }

    private void openInternal(String location, boolean stream) throws Exception {
        this.location = location;
        this.stream = stream;
        formatContext = avformat.avformat_alloc_context();
        AVDictionary options = new AVDictionary(null);
        if (stream) {
            avutil.av_dict_set(options, "rw_timeout", "10000000", 0);   // 10 s
            avutil.av_dict_set(options, "reconnect", "1", 0);
            avutil.av_dict_set(options, "reconnect_streamed", "1", 0);
            avutil.av_dict_set(options, "user_agent", "DAPlayer", 0);
        }
        int openResult = avformat.avformat_open_input(formatContext, location, null, options);
        avutil.av_dict_free(options);
        if (openResult < 0) {
            formatContext = null; // wurde von FFmpeg bereits freigegeben
            throw new Exception("Datei/Stream konnte nicht geöffnet werden: " + location + " (" + errorText(openResult) + ")");
        }
        if (avformat.avformat_find_stream_info(formatContext, (PointerPointer) null) < 0) {
            throw new Exception("Keine Stream-Informationen gefunden.");
        }
        audioStreamIndex = -1;
        for (int i = 0; i < formatContext.nb_streams(); i++) {
            if (formatContext.streams(i).codecpar().codec_type() == avutil.AVMEDIA_TYPE_AUDIO) {
                audioStreamIndex = i;
                break;
            }
        }
        if (audioStreamIndex < 0) {
            throw new Exception("Kein Audiostream gefunden.");
        }
        AVStream audioStream = formatContext.streams(audioStreamIndex);
        int codecId = audioStream.codecpar().codec_id();
        AVCodec codec = avcodec.avcodec_find_decoder(codecId);
        if (codec == null) {
            throw new Exception("Kein Decoder für Codec-ID " + codecId + " gefunden.");
        }
        codecContext = avcodec.avcodec_alloc_context3(codec);
        if (codecContext == null) {
            throw new Exception("AVCodecContext konnte nicht erstellt werden.");
        }
        if (avcodec.avcodec_parameters_to_context(codecContext, audioStream.codecpar()) < 0) {
            throw new Exception("Codec-Parameter konnten nicht übernommen werden.");
        }
        if (avcodec.avcodec_open2(codecContext, codec, (PointerPointer) null) < 0) {
            throw new Exception("Decoder konnte nicht geöffnet werden: " + codec.name().getString());
        }
        int sampleRate = codecContext.sample_rate();
        int channels = codecContext.ch_layout().nb_channels();
        if (sampleRate <= 0 || channels <= 0) {
            throw new Exception("Ungültiges Audioformat (" + sampleRate + " Hz, " + channels + " Kanäle).");
        }
        outChannels = Math.min(channels, 2);
        outSampleRate = sampleRate;
        outLayout = new AVChannelLayout();
        avutil.av_channel_layout_default(outLayout, outChannels);
        format = new AudioFormat(outSampleRate, 16, outChannels, true, false);

        packet = avcodec.av_packet_alloc();
        frame = avutil.av_frame_alloc();
        if (packet == null || frame == null) {
            throw new Exception("FFmpeg Speicher konnte nicht angelegt werden.");
        }
        inputEof = false;
        decoderEof = false;
        pending = new byte[0];
        pendingOffset = 0;
        skipUntilMillis = -1;
    }

    @Override
    public int decode(byte[] buffer) throws Exception {
        while (pendingOffset >= pending.length) {
            if (!fillPending()) {return -1;}
        }
        int frameSize = outChannels * 2;
        int available = pending.length - pendingOffset;
        int toCopy = Math.min(available, buffer.length - buffer.length % frameSize);
        System.arraycopy(pending, pendingOffset, buffer, 0, toCopy);
        pendingOffset += toCopy;
        return toCopy;
    }

    /** Holt den nächsten dekodierten Block nach {@link #pending}. @return false am Ende. */
    private boolean fillPending() throws Exception {
        if (decoderEof) {return false;}
        while (true) {
            int result = avcodec.avcodec_receive_frame(codecContext, frame);
            if (result >= 0) {
                byte[] converted = convertFrame();
                avutil.av_frame_unref(frame);
                if (converted.length > 0) {
                    pending = converted;
                    pendingOffset = 0;
                    return true;
                }
                continue;
            }
            if (result == avutil.AVERROR_EOF()) {
                decoderEof = true;
                byte[] rest = flushResampler();
                pending = rest;
                pendingOffset = 0;
                return rest.length > 0;
            }
            if (result != avutil.AVERROR_EAGAIN()) {
                throw new Exception("Fehler beim Dekodieren: " + errorText(result));
            }
            // Decoder braucht neue Daten
            if (inputEof) {
                decoderEof = true;
                return false;
            }
            result = avformat.av_read_frame(formatContext, packet);
            if (result < 0) {
                inputEof = true;
                avcodec.avcodec_send_packet(codecContext, (AVPacket) null); // Decoder leeren
                continue;
            }
            if (packet.stream_index() == audioStreamIndex) {
                avcodec.avcodec_send_packet(codecContext, packet); // fehlerhafte Pakete werden ignoriert
            }
            avcodec.av_packet_unref(packet);
        }
    }

    private byte[] convertFrame() throws Exception {
        int inSamples = frame.nb_samples();
        if (inSamples <= 0) {return new byte[0];}
        int inChannels = frame.ch_layout().nb_channels();
        if (inChannels <= 0) {inChannels = codecContext.ch_layout().nb_channels();}
        int inRate = frame.sample_rate() > 0 ? frame.sample_rate() : codecContext.sample_rate();
        int inFormat = frame.format();
        ensureResampler(inFormat, inRate, inChannels);

        int planes = avutil.av_sample_fmt_is_planar(inFormat) != 0 ? inChannels : 1;
        if (planes > 8) { // AV_NUM_DATA_POINTERS
            throw new Exception("Zu viele Kanäle: " + inChannels);
        }
        BytePointer[] in = new BytePointer[planes];
        for (int i = 0; i < planes; i++) {in[i] = frame.data(i);}
        PointerPointer<BytePointer> inPointer = new PointerPointer<>(in);
        try {
            int maxOut = swresample.swr_get_out_samples(swrContext, inSamples);
            ensureOutBuffer(Math.max(maxOut, inSamples) + 32);
            int converted = swresample.swr_convert(swrContext, outPointer, outBufferSamples, inPointer, inSamples);
            if (converted < 0) {throw new Exception("Resampling fehlgeschlagen: " + errorText(converted));}
            byte[] bytes = new byte[converted * outChannels * 2];
            outBuffer.position(0).get(bytes, 0, bytes.length);
            return trimForSeek(bytes);
        } finally {
            inPointer.close();
        }
    }

    /** Verwirft nach einem Seek die Samples vor der Zielposition (Seek springt auf den vorherigen Keyframe). */
    private byte[] trimForSeek(byte[] bytes) {
        if (skipUntilMillis < 0) {return bytes;}
        long pts = frame.best_effort_timestamp();
        if (pts == avutil.AV_NOPTS_VALUE) {pts = frame.pts();}
        if (pts == avutil.AV_NOPTS_VALUE) {
            skipUntilMillis = -1;
            return bytes;
        }
        AVRational tb = formatContext.streams(audioStreamIndex).time_base();
        long startMillis = tb.den() == 0 ? 0 : pts * 1000L * tb.num() / tb.den();
        long startOffset = formatContext.start_time() != avutil.AV_NOPTS_VALUE ? formatContext.start_time() / 1000L : 0;
        startMillis -= startOffset;
        if (startMillis >= skipUntilMillis) {
            skipUntilMillis = -1;
            return bytes;
        }
        int frameSize = outChannels * 2;
        long dropFrames = (skipUntilMillis - startMillis) * outSampleRate / 1000L;
        long dropBytes = dropFrames * frameSize;
        if (dropBytes >= bytes.length) {return new byte[0];}
        skipUntilMillis = -1;
        byte[] rest = new byte[bytes.length - (int) dropBytes];
        System.arraycopy(bytes, (int) dropBytes, rest, 0, rest.length);
        return rest;
    }

    private void ensureResampler(int inFormat, int inRate, int inChannels) throws Exception {
        if (swrContext != null && inFormat == swrInFormat && inRate == swrInRate && inChannels == swrInChannels) {return;}
        freeResampler();
        AVChannelLayout inLayout = new AVChannelLayout();
        try {
            AVChannelLayout frameLayout = frame.ch_layout();
            if (frameLayout.nb_channels() == inChannels && frameLayout.order() != avutil.AV_CHANNEL_ORDER_UNSPEC) {
                avutil.av_channel_layout_copy(inLayout, frameLayout);
            } else {
                avutil.av_channel_layout_default(inLayout, inChannels);
            }
            swrContext = swresample.swr_alloc();
            if (swrContext == null) {throw new Exception("SwrContext konnte nicht angelegt werden.");}
            int result = swresample.swr_alloc_set_opts2(swrContext, outLayout, avutil.AV_SAMPLE_FMT_S16, outSampleRate,
                                                        inLayout, inFormat, inRate, 0, null);
            if (result < 0) {throw new Exception("SwrContext konnte nicht konfiguriert werden: " + errorText(result));}
            if (swresample.swr_init(swrContext) < 0) {throw new Exception("SwrContext konnte nicht initialisiert werden.");}
            swrInFormat = inFormat;
            swrInRate = inRate;
            swrInChannels = inChannels;
        } finally {
            avutil.av_channel_layout_uninit(inLayout);
            inLayout.close();
        }
    }

    private void ensureOutBuffer(int samples) {
        if (outBuffer != null && outBufferSamples >= samples) {return;}
        if (outPointer != null) {outPointer.close();}
        if (outBuffer != null) {outBuffer.close();}
        outBufferSamples = samples;
        outBuffer = new BytePointer((long) samples * outChannels * 2);
        outPointer = new PointerPointer<>(outBuffer);
    }

    private byte[] flushResampler() {
        if (swrContext == null || outBuffer == null) {return new byte[0];}
        int converted = swresample.swr_convert(swrContext, outPointer, outBufferSamples, (PointerPointer) null, 0);
        if (converted <= 0) {return new byte[0];}
        byte[] bytes = new byte[converted * outChannels * 2];
        outBuffer.position(0).get(bytes, 0, bytes.length);
        return bytes;
    }

    private void freeResampler() {
        if (swrContext != null) {swresample.swr_free(swrContext); swrContext = null;}
        swrInFormat = -1;
        swrInRate = -1;
        swrInChannels = -1;
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public boolean isSeekable() {
        if (formatContext == null) {return false;}
        if (!stream) {return true;}
        return formatContext.duration() > 0 && formatContext.duration() != avutil.AV_NOPTS_VALUE;
    }

    @Override
    public void seekToMillis(long positionMillis) throws Exception {
        if (formatContext == null || codecContext == null || !isSeekable()) {return;}
        positionMillis = Math.max(0, positionMillis);
        AVStream audioStream = formatContext.streams(audioStreamIndex);
        AVRational millis = new AVRational().num(1).den(1000);
        long timestamp = avutil.av_rescale_q(positionMillis, millis, audioStream.time_base());
        millis.close();
        if (audioStream.start_time() != avutil.AV_NOPTS_VALUE) {timestamp += audioStream.start_time();}
        int result = avformat.av_seek_frame(formatContext, audioStreamIndex, timestamp, avformat.AVSEEK_FLAG_BACKWARD);
        if (result < 0) {
            throw new Exception("Seek auf " + positionMillis + " ms fehlgeschlagen.");
        }
        avcodec.avcodec_flush_buffers(codecContext);
        freeResampler();
        inputEof = false;
        decoderEof = false;
        pending = new byte[0];
        pendingOffset = 0;
        skipUntilMillis = positionMillis;
    }

    @Override
    public void reset() throws Exception {
        String loc = location;
        boolean wasStream = stream;
        close();
        openInternal(loc, wasStream);
    }

    @Override
    public void close() throws Exception {
        freeResampler();
        if (outPointer != null) {outPointer.close(); outPointer = null;}
        if (outBuffer != null) {outBuffer.close(); outBuffer = null;}
        outBufferSamples = 0;
        if (outLayout != null) {avutil.av_channel_layout_uninit(outLayout); outLayout.close(); outLayout = null;}
        if (frame != null) {avutil.av_frame_free(frame); frame = null;}
        if (packet != null) {avcodec.av_packet_free(packet); packet = null;}
        if (codecContext != null) {avcodec.avcodec_free_context(codecContext); codecContext = null;}
        if (formatContext != null) {avformat.avformat_close_input(formatContext); formatContext = null;}
        pending = new byte[0];
        pendingOffset = 0;
        audioStreamIndex = -1;
    }

    private static String errorText(int code) {
        BytePointer buffer = new BytePointer(256);
        try {
            avutil.av_strerror(code, buffer, 256);
            return buffer.getString();
        } catch (Throwable t) {
            return "Fehlercode " + code;
        } finally {
            buffer.close();
        }
    }
}
