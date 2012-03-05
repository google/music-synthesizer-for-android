{
  'targets': [
    {
      'target_name': 'core',
      'type': 'static_library',
      'sources': [
        'dx7note.cc',
        'env.cc',
        'fm_core.cc',
        'fm_op_kernel.cc',
        'freqlut.cc',
        'resofilter.cc',
        'ringbuffer.cc',
        'sawtooth.cc',
        'sin.cc',
        'synth_unit.cc',
        'test_ringbuffer.cc',
      ],
      'include_dirs': ['.'],
    },
  ],
}

