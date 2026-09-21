# DAPlayer

DAPlayer is a JavaFX-based desktop audio player developed by emgeem.

The project is currently in active development. Version **0.1.0** represents the first documented functional development stage of the player.

## Features

### Playback

* WAV playback
* MP3 playback
* Play / Pause / Resume
* Stop
* Automatic transition to the next track
* Automatic stop after the last track
* Volume control
* Mute
* Audio output device selection

### Playlist and Track Navigation

* Create and manage a playlist
* Open individual audio files
* Open folders containing audio files
* Select tracks from the playlist
* Previous / next track navigation
* Automatic track progression

### Track Information

The Track Info display provides information such as:

* Title
* Artist
* Album
* Album Artist
* Genre
* Year
* Track number
* Disc number
* Format
* Duration

Album artwork is displayed when artwork is available.

### Playback Progress and Seeking

DAPlayer provides a playback progress display including:

* Current playback position
* Total track duration
* Visual playback progress
* Mouse-based seeking

Seeking is currently implemented for:

* WAV
* MP3

### User Interface

The player uses a custom JavaFX interface with separate display areas for:

* Start screen
* Track list
* Track information
* Settings

The interface also contains custom controls for playback, volume and navigation.

### Settings

The current settings area provides:

* Audio device selection
* Accent color configuration

## Supported Audio Formats

Currently implemented:

* WAV
* MP3

Additional formats may be added during future development.

## Technology

DAPlayer is currently built with:

* Java 21
* JavaFX 24.0.2
* Maven
* JAudioTagger 3.0.1
* JLayer 1.0.1

## Project Structure

```text
src/main/java/de/emgeem/daplayer/
│
├── audio/
│   ├── AudioDeviceManager
│   ├── AudioEngine
│   └── PlayerController
│
├── decoder/
│   ├── AudioDecoder
│   ├── DecoderManager
│   ├── Mp3Decoder
│   └── WavDecoder
│
├── model/
│   ├── Playlist
│   └── Track
│
└── ui/
    ├── DAPlayerView
    │
    ├── background/
    ├── control/
    ├── display/
    └── painter/
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

Version **0.1.0** provides the basic foundation of the DAPlayer, including a functional user interface, playlist handling, WAV and MP3 playback, playback controls, progress display and seeking.

The project is still under active development. Architecture, user interface and playback functionality may change in future versions.

## Planned Development

Possible future development includes additional audio formats and further expansion of the player functionality and user interface.
