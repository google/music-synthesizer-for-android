{
  'targets': [
    {
      'target_name': 'SynthApp',
      'type': 'executable',
      'mac_bundle': 1,
      'include_dirs': ['.'],
      'sources': [
        'SynthApp/main.m',
        'SynthApp/midi_in_mac.cc',
        'SynthApp/SynthAppDelegate.mm',
        'SynthApp/SynthApp_Prefix.pch',
        'SynthApp/SynthMain.mm',
      ],
      'dependencies': [
        'core.gyp:core',
      ],
      'link_settings': {
        'libraries': [
          '$(SDKROOT)/System/Library/Frameworks/AudioToolbox.framework',
          '$(SDKROOT)/System/Library/Frameworks/AudioUnit.framework',
          '$(SDKROOT)/System/Library/Frameworks/Carbon.framework',
          '$(SDKROOT)/System/Library/Frameworks/Cocoa.framework',
          '$(SDKROOT)/System/Library/Frameworks/CoreAudio.framework',
          '$(SDKROOT)/System/Library/Frameworks/CoreFoundation.framework',
          '$(SDKROOT)/System/Library/Frameworks/CoreMIDI.framework',
        ],
      },
      'xcode_settings': {
        'INFOPLIST_FILE': 'SynthApp/Synth-Info.plist',
      },
      'mac_bundle_resources': [
        'SynthApp/English.lproj/InfoPlist.strings',
        'SynthApp/English.lproj/MainMenu.xib',
      ],
    },
  ],
}
