# DAPlayer

DAPlayer is a JavaFX-based desktop audio player for playing and managing
WAV, MP3 and WMA audio files.

The project is developed as a native desktop application and is intended
to run locally without SaaS dependencies.

## Version

Current version: **0.2.0**

## Features

### Audio Playback

- WAV playback
- MP3 playback
- WMA playback
- Play / Pause / Resume
- Stop
- Automatic transition to the next track
- Automatic stop at the end of the playlist
- Volume control
- Mute functionality
- Audio output device selection
- Shuffle playback mode
- Repeat playback mode

### Playlist

- File chooser
- Folder-based file selection
- Playlist management
- Track selection
- Track navigation
- Automatic next-track playback
- Shuffle playback
- Repeat playback

### Track Information

- Track metadata display
- Album artwork display
- Track list
- Track information display
- Current playback position
- Total track duration

### Playback Progress

- Visual playback progress bar
- Mouse-based seeking
- WAV seeking
- MP3 seeking
- WMA seeking

### Equalizer

DAPlayer includes a real-time 10-band graphic equalizer.

Available frequency bands:

- 31 Hz
- 62 Hz
- 125 Hz
- 250 Hz
- 500 Hz
- 1 kHz
- 2 kHz
- 4 kHz
- 8 kHz
- 16 kHz

Gain range:

- -12 dB to +12 dB

Equalizer settings are stored persistently using Java Preferences.

### Audio Visualizations

DAPlayer includes a dedicated Visuals display with four visualization modes:

- Spectrum
- Waveform
- VU-Meter
- Vectorscope

The visualization system receives real-time audio data from the audio engine.

The VU-Meter uses separate left and right channel peak and RMS values.

The Vectorscope uses separate left and right channel sample data for
stereo visualization.

DAPlayer also supports an RGB visualization mode.

### User Interface

The player uses a custom JavaFX interface with:

- Custom player background
- Custom volume knob
- Playback control buttons
- Menu buttons
- Off and navigation buttons
- Start display
- Track list display
- Track information display
- Settings display
- Equalizer display
- Visuals display
- Custom accent colors
- Playback state indication

The playback controls provide visual feedback for:

- PLAYING
- PAUSED
- STOPPED

## Settings

### Audio Device

DAPlayer allows the user to select the audio output device.

### Accent Colors

The accent color can be changed through the Settings display.

The selected accent color is stored persistently.

### Equalizer

Equalizer settings are stored persistently and restored when DAPlayer
is started again.

## Supported Audio Formats

Currently supported:

- WAV
- MP3
- WMA

### WMA

WMA playback is implemented using FFmpeg through JavaCPP.

Supported WMA functionality includes:

- Audio decoding
- Metadata reading
- Track duration
- Playback
- Seeking
- Pause / Resume
- Automatic track transition

## Technologies

DAPlayer is built using:

- Java 21
- JavaFX 24.0.2
- Maven
- JAudioTagger 3.0.1
- JLayer 1.0.1
- FFmpeg 8.1.2 via JavaCPP

## Project Structure

```text
src/
└── main/
    └── java/
        └── de/
            └── emgeem/
                └── daplayer/
                    ├── audio/
                    │   ├── AudioDeviceManager
                    │   ├── AudioEngine
                    │   ├── DecoderManager
                    │   ├── Equalizer
                    │   ├── EqualizerSettings
                    │   ├── PlayerController
                    │   ├── VisualizationData
                    │   └── VisualizationListener
                    │
                    ├── decoder/
                    │   ├── AudioDecoder
                    │   ├── Mp3Decoder
                    │   ├── WavDecoder
                    │   └── WmaDecoder
                    │
                    ├── model/
                    │   ├── Playlist
                    │   └── Track
                    │
                    └── ui/
                        ├── DAPlayerView
                        ├── PlayerDisplay
                        ├── PlayerPanel
                        └── display/
                            ├── StartDisplay
                            ├── TrackListDisplay
                            ├── TrackInfoDisplay
                            ├── SettingsDisplay
                            ├── EqualizerDisplay
                            ├── VisualsDisplay
                            └── visual/
                                ├── SpectrumVisualization
                                ├── WaveformVisualization
                                ├── VuVisualization
                                └── VectorScope
```

Resources such as fonts and application icons are located under:

```text
src/main/resources/
```

## Prerequisites

- Java JDK 21 or higher
- Maven 3.9.x or higher

DAPlayer uses JLayer 1.0.1 for MP3 playback.
Since this version is not normally obtained via Maven Central, the
required JAR file is located in the `lib/` directory.

## Installation

Clone or download the repository.

Then, install JLayer 1.0.1 into the local Maven repository:

```bash
mvn install:install-file "-Dfile=lib/jlayer-1.0.1.jar" "-DgroupId=javazoom" "-DartifactId=jlayer" "-Dversion=1.0.1" "-Dpackaging=jar"
```

## Building

The project uses Maven.

To compile the project:

```bash
mvn clean package
```

To start the application using the JavaFX Maven plugin:

```bash
mvn javafx:run
```

## Current Development Status

Version **0.2.0** currently provides:

- WAV playback
- MP3 playback
- WMA playback
- Playlist handling
- Track navigation
- Automatic next-track playback
- Automatic playlist stop
- Shuffle playback
- Repeat playback
- Volume control
- Mute functionality
- Audio device selection
- Track information
- Album artwork
- Playback progress
- WAV seeking
- MP3 seeking
- WMA seeking
- Audio equalizer
- Persistent equalizer settings
- Spectrum visualization
- Waveform visualization
- VU-Meter visualization
- Vectorscope visualization
- RGB visualization mode
- Audio device settings
- Accent color settings
- Playback state indication

The project is still under active development. Architecture, user interface and playback functionality may change in future versions.

## Planned Development

Possible future development includes:

- Additional audio formats
- Music library functionality
- Further expansion of the player functionality
- Further expansion of the user interface
