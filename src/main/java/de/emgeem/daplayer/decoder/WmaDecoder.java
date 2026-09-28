package de.emgeem.daplayer.decoder;

import java.io.File;

import javax.sound.sampled.AudioFormat;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.PointerPointer;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

import de.emgeem.daplayer.model.Track;

public class WmaDecoder implements AudioDecoder {

    private File file;
    private AudioFormat format;
    private AVFormatContext formatContext;
    private AVCodecContext codecContext;
    private AVPacket packet;
    private AVFrame frame;
    private SwrContext swrContext;
    private int audioStreamIndex = -1;
    private byte[] pcmBuffer = new byte[0];
    private int pcmBufferOffset = 0;

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".wma");
    }

    @Override
    public Track readTrack(File file) {
        Track track = new Track(file);
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            if (audioFile.getAudioHeader() != null) {
                int durationSeconds = audioFile.getAudioHeader().getTrackLength();
                track.setDuration(durationSeconds * 1000L);
            }
            Tag tag = audioFile.getTag();
            if (tag != null) {
                track.setTitle(tag.getFirst("TIT2"));
                if (track.getTitle() == null || track.getTitle().isBlank()) {track.setTitle(track.getFileName());}
                track.setArtist(tag.getFirst("TPE1"));
                track.setAlbum(tag.getFirst("TALB"));
                track.setAlbumArtist(tag.getFirst("TPE2"));
                track.setGenre(tag.getFirst("TCON"));
                String year = tag.getFirst("TYER");
                if (year != null && !year.isBlank()) {track.setYear(Integer.parseInt(year));}
                String trackNumber = tag.getFirst("TRCK");
                if (trackNumber != null && !trackNumber.isBlank()) {track.setTrackNumber(parseNumber(trackNumber));}
                String discNumber = tag.getFirst("TPOS");
                if (discNumber != null && !discNumber.isBlank()) {track.setDiscNumber(parseNumber(discNumber));}
            }
        } catch (Exception e) {
            System.err.println("Could not read WMA: " + file.getName());
            e.printStackTrace();
        }
        return track;
    }

    @Override
    public void open(File file) throws Exception {
        this.file = file;
        formatContext = avformat.avformat_alloc_context();
        if (avformat.avformat_open_input(formatContext, file.getAbsolutePath(), null, null) < 0) {
            throw new Exception("WMA-Datei konnte nicht geöffnet werden: " + file.getAbsolutePath());
        }
        if (avformat.avformat_find_stream_info(formatContext, (PointerPointer) null) < 0) {
            throw new Exception("Keine Stream-Informationen gefunden.");
        }
        for (int i = 0; i < formatContext.nb_streams(); i++) {
            AVStream stream = formatContext.streams(i);
            if (stream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_AUDIO) {
                audioStreamIndex = i;
                break;
            }
        }
        if (audioStreamIndex < 0) {
            throw new Exception("Kein Audiostream gefunden.");
        }
        AVStream audioStream = formatContext.streams(audioStreamIndex);
        int codecId = audioStream.codecpar().codec_id();
        var codec = avcodec.avcodec_find_decoder(codecId);
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
            throw new Exception("WMA-Decoder konnte nicht geöffnet werden.");
        }
        int sampleRate = codecContext.sample_rate();
        int channels = codecContext.ch_layout().nb_channels();
        format = new AudioFormat(sampleRate, 16, channels, true, false);
        System.out.println("WMA geöffnet:");
        System.out.println("  Sample Rate: " + sampleRate);
        System.out.println("  Channels:    " + channels);
        System.out.println("  Codec:       " + codec.name().getString());
        packet = avcodec.av_packet_alloc();
        frame = avutil.av_frame_alloc();
        if (packet == null || frame == null) {
            throw new Exception("FFmpeg Speicher konnte nicht angelegt werden.");
        }
        swrContext = swresample.swr_alloc();
        if (swrContext == null) {
            throw new Exception("SwrContext konnte nicht angelegt werden.");
        }
        AVChannelLayout channelLayout = codecContext.ch_layout();
        int result = swresample.swr_alloc_set_opts2(swrContext, channelLayout, avutil.AV_SAMPLE_FMT_S16, codecContext.sample_rate(),
                                                    channelLayout, codecContext.sample_fmt(), codecContext.sample_rate(), 0, null);
        if (result < 0) {
            throw new Exception("SwrContext konnte nicht konfiguriert werden.");
        }
        if (swresample.swr_init(swrContext) < 0) {
            throw new Exception("SwrContext konnte nicht initialisiert werden.");
        }
    }

    @Override
    public int decode(byte[] buffer) throws Exception {
        if (pcmBufferOffset < pcmBuffer.length) {
            int available = pcmBuffer.length - pcmBufferOffset;
            int bytesToCopy = Math.min(buffer.length, available);
            System.arraycopy(pcmBuffer, pcmBufferOffset, buffer, 0, bytesToCopy);
            pcmBufferOffset += bytesToCopy;
            return bytesToCopy;
        }
        while (true) { 
            int result = avformat.av_read_frame(formatContext, packet);
            if (result < 0) {return -1;}
            if (packet.stream_index() != audioStreamIndex) {
                avcodec.av_packet_unref(packet);
                continue;
            }
            result = avcodec.avcodec_send_packet(codecContext, packet);
            avcodec.av_packet_unref(packet);
            if (result < 0) {continue;}
            result = avcodec.avcodec_receive_frame(codecContext, frame);
            if (result == avutil.AVERROR_EAGAIN() || result == avutil.AVERROR_EOF()) {continue;}
            if (result < 0) {
                throw new Exception("Fehler beim Decodieren des WMA-Frames.");
            }
            int sampleCount = frame.nb_samples();
            int channels = codecContext.ch_layout().nb_channels();
            int outputBytes = sampleCount * channels * 2;
            byte[] converted = new byte[outputBytes];
            org.bytedeco.javacpp.FloatPointer left = new org.bytedeco.javacpp.FloatPointer(frame.data(0));
            org.bytedeco.javacpp.FloatPointer right = null;
            if (channels > 1 && frame.data(1) != null) {right = new org.bytedeco.javacpp.FloatPointer(frame.data(1));}
            for (int i = 0; i < sampleCount; i++) {
                float leftSample = left.get(i);
                if (leftSample > 1.0f) leftSample = 1.0f;
                if (leftSample < -1.0f) leftSample = -1.0f;
                short leftPcm = (short) (leftSample * 32767.0f);
                int offset = i * channels * 2;
                converted[offset] = (byte) (leftPcm & 0xFF);
                converted[offset + 1] = (byte) ((leftPcm >> 8) & 0xFF);
                if (channels > 1) {
                    float rightSample = right != null ? right.get(i) : leftSample;
                    if (rightSample > 1.0f) rightSample = 1.0f;
                    if (rightSample < -1.0f) rightSample = -1.0f;
                    short rightPcm = (short) (rightSample * 32767.0f);
                    converted[offset + 2] = (byte) (rightPcm & 0xFF);
                    converted[offset + 3] = (byte) ((rightPcm >> 8) & 0xFF);
                }
            }
            pcmBuffer = converted;
            pcmBufferOffset = 0;
            int bytesToCopy = Math.min(buffer.length, converted.length);
            System.arraycopy(pcmBuffer, 0, buffer, 0, bytesToCopy);
            pcmBufferOffset = bytesToCopy;
            return bytesToCopy;
        }
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public void seekToMillis(long positionMillis) throws Exception {
        if (formatContext == null || codecContext == null) {return;}
        positionMillis = Math.max(0, positionMillis);
        AVStream audioStream = formatContext.streams(audioStreamIndex);
        long timestamp = avutil.av_rescale_q(positionMillis, new org.bytedeco.ffmpeg.avutil.AVRational().num(1).den(1000), audioStream.time_base());
        int result = avformat.av_seek_frame(formatContext, audioStreamIndex, timestamp, avformat.AVSEEK_FLAG_BACKWARD);
        if (result < 0) {
            throw new Exception("Seek auf " + positionMillis + " ms fehlgeschlagen.");
        }
        avcodec.avcodec_flush_buffers(codecContext);
        pcmBuffer = new byte[0];
        pcmBufferOffset = 0;
    }

    @Override
    public void reset() throws Exception {
        close();
        open(file);
    }

    @Override
    public void close() throws Exception {
        if (swrContext != null) {swresample.swr_free(swrContext); swrContext = null;}
        if (frame != null) {avutil.av_frame_free(frame); frame = null;}
        if (packet != null) {avcodec.av_packet_free(packet); packet = null;}
        if (codecContext != null) {avcodec.avcodec_free_context(codecContext); codecContext = null;}
        if (formatContext != null) {avformat.avformat_close_input(formatContext); formatContext = null;}
        pcmBuffer = new byte[0];
        pcmBufferOffset = 0;
        audioStreamIndex = -1;
    }

    private int parseNumber(String value) {
        try {
            int slash = value.indexOf('/');
            if (slash >= 0) {value = value.substring(0, slash);}
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}