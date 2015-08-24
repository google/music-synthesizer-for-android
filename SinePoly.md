# Sine generation: polynomials #

There are two techniques used in the synthesizer for generating sine
waves. The scalar code uses a 1024-element lookup table with linear
interpolation, but the NEON code uses a polynomial. Polynomial evaluation
parallelizes easily, unlike lookups.

Most math library implementations of sin() give results accurate to a
couple of LSB's of floating point accuracy. But that's overkill for our
purposes. We want a sine so that the errors are just below what you can
hear. A sixth order polynomial is a good choice here, as the loudest
harmonic (the 3rd) is almost 100dB down from the fundamental.
Also, harmonic distortion in low frequencies is "musical". It seems silly
to go to a huge amount of trouble to suppress harmonics and create a
pure sine tone, when in practice these sines are going to be assembled
into an FM modulation graph to make rich harmonics. However, high
frequency noise is bad because it will create aliasing.

The usual criterion for designing a polynomial for function approximation
is to minimize the worst case error. But for this application, not all
error is created equal. We're willing to tolerate a small increase in
absolute error if we can shape the spectrum to concentrate the error
mostly in the low frequencies.

The design we ended up with was to compute a minimum absolute error
for a 5-th order polynomial, then integrate it. The result is:

```
    y = 1 - 1.2333439964934032 * x**2 + 0.25215252666796095 * x**4 - 0.01880853017455781 * x**6
```

In this graph of the error compared to true sine, you can see the difference.
The absolute error computed with Chebyshev fitting is smaller, but there's
a discontinuity when the sign flips, and the frequency gets high. The
"smooth" variant gets rid of the discontinuity, and the high frequency
ripples get attenuated, of course at the cost of the absolute error being
higher.

![http://wiki.music-synthesizer-for-android.googlecode.com/git/img/cheby_vs_smooth.png](http://wiki.music-synthesizer-for-android.googlecode.com/git/img/cheby_vs_smooth.png)

The spectrum tells a similar story: the tail of the "smooth" variant has
about 10dB less energy than the Chebyshev fit, while the low frequency harmonics
are a touch higher.

![http://wiki.music-synthesizer-for-android.googlecode.com/git/img/cheby_vs_smooth_fr.png](http://wiki.music-synthesizer-for-android.googlecode.com/git/img/cheby_vs_smooth_fr.png)

This was fun to to, as it felt like an optimization across all levels of
the stack, down to cycle counts on the NEON code, and all the way up to
how musical the tones would sound.

# References #

  * [Fun with Sinusoids](http://www.rossbencina.com/code/sinusoids) by Ross Bencina

  * [Chebyshev approximation in Python](http://www.excamera.com/sphinx/article-chebyshev.html) — the simple but effective tool I used to compute the polynomials

  * [Pulsar cycle counter](http://pulsar.webshaker.net/ccc/sample-506402de) with scheduling analysis of resulting NEON code for the inner loop (each iteration computes 12 values of the FM synthesis kernel, of which the bulk of the calculation is the sine)
