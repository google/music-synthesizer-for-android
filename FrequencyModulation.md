# FM Synthesis #

Frequency modulation (FM) synthesis is the core technique used by the Yamaha DX7 to produce sounds. It was originally invented by John Chowning around 1966, published in 1973 (see below), refined throughout the '70s (with Yamaha producing the innovative but not particularly successful [GS-1](http://www.synthtopia.com/content/2010/03/05/yamaha-gs-1/), massing 90kg and relying on 50 discrete IC's to compute the sound - and using magnetic strip memory to store patches), and achieving mass popularity for the first time in the DX7.

There is some terminological confusion around FM synthesis, with some believing that a more correct term would be "phase modulation" or "phase distortion." The two concepts are very closely related, as phase modulation by a signal y(x) is equivalent to frequency modulation by dy/dx. In the basic case where the modulating signal is a sine wave (so the derivative is also a sine wave, albeit with different phase and amplitude). Part of the confusion, no doubt, is due to Casio's use of "Phase Distortion" terminology for their competing CZ line of synthesizers.

For the most part, the DX7 implements pure FM synthesis, using six operators for each voice, in 32 possible configurations (known as "algorithms" in Yamaha lingo). However, it also implements "feedback FM", an innovation by Tomisawa that expands the range of waveforms and spectra available. Feedback FM produces a waveform resembling a sawtooth wave (very familiar in analog synthesizers and their digital modeling counterparts), with monotonically decreasing amplitudes of the overtones, as opposed to the wavelet-like shape of spectra (deriving from Bessel functions) of standard FM. Also, when driven at very high loop gains, feedback FM can become chaotic and white-noise like.

The mathematics and history of FM synthesis are authoritatively covered in the links below, which together make excellent reading.

# References #

  * [The Synthesis of Complex Audio Spectra by Means of Frequency Modulation](http://users.ece.gatech.edu/~mcclella/2025/labs-s05/Chowning.pdf), John M. Chowning, J. AES 21(7), Sept. 1973
  * [Frequency modulation synthesis](http://en.wikipedia.org/wiki/Frequency_modulation_synthesis) at Wikipedia
  * [Interview with John Chowning](http://www.abdn.ac.uk/~mth192/html/Chowning.html), Aftertouch Magazine 1(2)
  * [An Introduction to FM](https://ccrma.stanford.edu/software/snd/snd/fm.html), Bill Schottstaedt, CCRMA (Stanford)
  * [Music: A Mathematical Offering](http://www.maths.abdn.ac.uk/~bensondj/html/music.pdf), Dave Benson, Cambridge University Press, Nov 2006