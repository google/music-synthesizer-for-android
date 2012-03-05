{
  'targets': [
    {
      'target_name': 'main',
      'type': 'executable',
      'sources': [
        'main.cc',
        'wavout.cc',
      ],
      'dependencies': [
        'core.gyp:core',
      ],
      'include_dirs': ['.'],
    },
  ],
}

