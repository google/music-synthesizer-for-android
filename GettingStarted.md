# Getting started with Music Synthesizer Development #

The following steps will get you started working with the Music Synthesizer for Android code in a Unix-like environment.  The following environment variables are used.

  * `SYNTH_PATH` - Location of the Music Synthesizer source code.
  * `PROTO_PATH` - Location where Protocol Buffers are installed.

## Installing Protocol Buffers ##

Download the Google [Protocol Buffer](http://code.google.com/p/protobuf/) package from [here](http://code.google.com/p/protobuf/downloads/list).

To build the `protoc` compiler, run the following commands.  If you are using Windows, you can skip this step by downloading the prebuilt Windows `protoc` compiler and installing it in `$SYNTH_PATH/music-synthesizer-for-android/core/bin/`.
```
tar -xzvf protobuf-2.4.0a.tar.gz
cd protobuf-2.4.0a
./configure --prefix=$PROTO_PATH
make
make check
make install
mkdir $SYNTH_PATH/music-synthesizer-for-android/core/bin/
cp $PROTO_PATH/bin/protoc $SYNTH_PATH/music-synthesizer-for-android/core/bin/
```

Build the protocol buffer runtime libraries jar.
```
cd java/
mvn test
mvn install
mvn package
mkdir $SYNTH_PATH/music-synthesizer-for-android/core/lib/
cp target/protobuf-java-2.4.*.jar $SYNTH_PATH/music-synthesizer-for-android/core/lib/libprotobuf.jar
```

## Installing Eclipse ##

Other development environments are unsupported.  However, the core, test, and j2se packages can be built using Ant.  So the desktop tools in the j2se package can still be built without Eclipse.

To download and install Eclipse, visit [eclipse.org](http://www.eclipse.org/downloads/).

## Installing the Android SDK ##

Download and Install the Android SDK using the instructions at [android.com](http://developer.android.com/sdk/index.html).

## Installing Music Synthesizer for Android ##

Using Git, download the Music Synthesizer for Android source code.  Visit [here](http://code.google.com/p/music-synthesizer-for-android/source/checkout) for more details.
```
git clone https://code.google.com/p/music-synthesizer-for-android/
```

## Testing Music Synthesizer for Android core components ##

To make sure everything so far is installed correctly, run the tests and make sure they all build and pass.
```
cd $SYNTH_PATH/music-synthesizer-for-android/
ant test
```

## Setting up NDK ##

The new synth engine is written in C++ for higher performance, and uses OpenSL ES to output sound. Install the [Android NDK](http://developer.android.com/sdk/ndk/index.html). Then, you can either manually run the ndk compile, or set up your Eclipse project to run it automatically.

To run it manually: make sure that ndk-build is on your path, go into the android subdirectory and run:

```
ndk-build
```

To set up automatic building, edit android/.externalToolBuilders/NDK Builder.launch to make sure that ATTR\_LOCATION points to a valid location for the ndk-build binary. The default is ${HOME}/install/android-ndk-r7b/ndk-build , so if you unpacked the NDK into the install subdirectory of your home directory, and the versions match, it may just work.

The result of the ndk-build step is to create a libsynth.so file containing the shared library. For example, android/libs/armeabi-v7a/libsynth.so.

The shared library build depends on the target architecture (unlike Java code). The default is armeabi-v7a, and can be changed by editing APP\_ABI in the android/jni/Application.mk file. Note that code built for armeabi will run on ARM v7 devices, but more slowly. It might make sense to set this to "all" so that it will run on more devices, but at the expense of slowing the compile cycle and potentially bloating the APK file size.

## Setting up Music Synthesizer in Eclipse ##
Make a new Eclipse workspace.  Import the project into Eclipse. This should be File > Import... > Android > Existing Android Code Into Workspace. You will probably get errors on import (duplicate entry 'src', empty ${project\_loc}, and maybe others). You can ignore these (although it would be great to clean them up).