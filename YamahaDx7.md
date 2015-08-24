# Overview #

The Yamaha DX7 was the first highly successful purely digital synthesizer, and the best selling synthesizer of its day.

It is based on FM synthesis, invented by John Chowning at Stanford University in the early 1970's. As with any synthesis technique, it has strengths and weaknesses, but the strengths make it particularly suitable as the basis for a synthesizer on the Android platform. One significant advantage is that a wide range of different sounds can be created and represented in a tiny amount of storage space - a DX7 patch is 128 bytes. In addition, generating the sound requires modest computing resources.

# Reverse Engineering #

There are a number of software implementations of the DX7 (most notably, FM7, Hexter, and a CSound translator), but all suffer from imperfect emulation of the original.

A major goal of the DX7 synthesis module in this project is to match the original as precisely as possible, or, in some cases, to surpass it in sound quality. To do this, we have a test framework which sends MIDI patches and test notes to a physical DX7s, and a sound capture rig (a Roland Quad-Capture) to capture the sound with very high quality and resolution (192ksamples/s, 24 bits). The goal is to understand and document the synthesis techniques used in the actual DX7 almost to the bit level.

Fortunately, this is an achievable goal. The actual synthesis is done by a pair of LSI chips (the YM21290 for envelope generation, and the YM21280 for generating the modulated sine waves), all controlled by an 8 bit microprocessor (a 68B03 in the original DX7, probably running at 2MHz, which was a variant of the Motorola 6800). None of this was capable of a huge amount of complexity. Thus, careful measurement can reveal all the secrets of this hardware. This work is in progress, and as it is completed, the results will be reported in subpages.

Much of the publicly available research on the DX7 is based on the DX7 to Csound translator work done by Jeff Harrington and Sylvain Marchand. However, there are numerous details which are inaccurate.

# Synthesis #

The new synthesis engine is designed with a number of goals in mind:

  * Top-notch sound quality, meeting or exceeding that of the original DX7
  * High performance, for good battery life and robust performance even on limited hardware
  * A portable C++ codebase, optimized for 32-bit fixed point arithmetic

The code draws ideas from a number of different sources, including the original DX7, Hexter, and the Sonivox FM synthesizer (which is now part of the Android source, at [external/sonivox/arm-fm-22k](https://github.com/android/platform_external_sonivox/tree/master/arm-fm-22k/lib_src)).

# Links #

  * [DX7 Wikipedia page](http://en.wikipedia.org/wiki/Yamaha_DX7)
  * [Dave Benson's DX7 page](http://www.abdn.ac.uk/~mth192/html/dx7.html)
  * [Hexter](http://dssi.sourceforge.net/hexter.html), another free software emulator
  * [Yamaha YM chips numerical classification](http://www.vorc.org/text/column/hally/ymxxxx.html)
  * [DX7 to Csound Translator](http://www.parnasse.com/dx72csnd.shtml)