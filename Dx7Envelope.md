# Interactive model #

Explore the interactive JavaScript [implementation](http://wiki.music-synthesizer-for-android.googlecode.com/git/img/env.html) of a nearly bit-accurate model of the DX7 envelope.

Also see [plots](http://wiki.music-synthesizer-for-android.googlecode.com/git/img/outlevel.html) of the scaling tables for both output level and rate, from the 0..99 values in DX7 patches to real-world values, measured in dB and dB/s.

# The DX7 Envelope #

This page contains a detailed description of the envelope generation in the DX7. Conceptually, there's an "idealized envelope" which has smooth variation of amplitude as a function of time, and then there's the actual realization in the DX7's hardware, which introduces various forms of quantization, mostly due to running with very small numbers of bits for state.

## Idealized envelope ##

The envelope logic is fairly simple, but also quite musically expressive. The main parameters are four levels and rates, and also the output level (described in a bit more detail below). The shape of the envelope is asymmetrical - while the decay portions are purely exponential, the attack portions are a more complex shape approximating linear. This asymmetry is visible in the envelope figures in Chowning's original paper on FM synthesis - see reference below. Chowning says, "A general characteristic of percussive sounds is that the decay shape of the envelope is roughly exponential as shown in Fig. 14", while the attacks shown in examples of envelopes for brass and woodwind sounds is much closer to linear.

<a href='Hidden comment: Figures from Chowning'></a>

The full state of the idealized envelope is represented as an _index_ of which part of the envelope is active (labeled 0-3 in this discussion), combined with a _level_. Typically, the envelope starts out at L4 and increases to L1. Then, when it reaches the _target_ of L1, the index increments, and it proceeds to L2 (either by decay or attack, depending on whether L1 or L2 is greater).

The conversion from level parameter (L1, L2, L3, L4 in the patch) to actual level is as follows:

```
Level 0..5 -> actual level = 2 * l
Level 5..16 -> actual level = 5 + l
Level 17..20 -> actual level = 4 + l
Level 20..99 -> actual level = 14 + (l >> 1)
```

The output level is scaled similarly, but is just 28 + l for values 20..99. It
has twice the precision of the level parameters. The exact lookup table for values 0..19 is [0, 5, 9, 13, 17, 20, 23, 25, 27, 29, 31, 33, 35, 37, 39,
41, 42, 43, 45, 46].

Then, the total level is 64 `*` actual level + 32 `*` output level, normalized
for full scale. This "total level" is in units of approx .0235 dB (20 log<sub>10</sub>(2) / 256), so that 256 of these steps doubles the amplitude.

From measurement of timing, the minimum level seems to be clipped at
3824 counts from full scale -> 14.9375 doublings. Note, however, that velocity values > 100 can cause amplitudes greater than full scale. Full scale means both L and output level set to 99 in the patch, and no additional scaling.

As mentioned above, the decay shape is simpler than the attack. An exponential decay corresponds to a linear change in dB units. First, the R parameter in the patch (range 0..99) is converted to a 6 bit value (0..63), by the formula qrate = (rate `*` 41) / 64.

The rate of decay is then 0.2819 `*` 2<sup>(qrate / 4)</sup> `*` (1 + 0.25 `*` (qrate mod 4)) dB/s. This is a reasaonably good approximation to 0.28 `*` 2<sup>(qrate <code>*</code> 0.25)</sup>.

Attack is based on decay, multiplying it by a factor dependent on the current level. In .0235 dB units, this factor is 2 + floor((full scale - current level) / 256). Also, level _immediately_ rises to 39.98 dB (1700 steps) above the minimum level, which helps create a crisper attack.

## Output level ##

The output level is computed once, at the beginning of the note, and affects both the overall amplitude of the operator and also the timing. In addition to the "output level" setting in the patch, output level is also affected by velocity and scaling.

The output level in the patch is in the range 0..99, and this is scaled in units of 0.7526 dB (ie 32 steps).

## Hardware ##

Careful measurement of the DX7 reveals quite rich detail on how envelopes are actually computed. Clearly the resolution for amplitude is .0235 dB, and there are 12 bits total (for a maximum dynamic range of 72.25 dB).

At a qrate of 0, the amplitude decreases by one step every 4096 samples, in other words halves every 2<sup>20</sup> samples. Each increase of 4 doubles the clock rate. Careful examination reveals that fractional multiples of qrate (ie qrate is not a multiple of 4) are clocked out using a pattern:

```
01010101
01010111
01110111
01111111
```

For attacks, instead of decrementing by 1, the factor is added (thus, no actual multiplication is needed). When the clock rate increases to the point where the increment would be needed more than once per sample clock (ie for qrate >= 48), the increment value is shifted left by (qrate / 4) - 11 instead, and the increment (masked by the bit pattern above) is applied every single sample clock.

# References #

