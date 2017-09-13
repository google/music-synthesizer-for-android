/*
 * Copyright 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "SynthMain.h"

#include <iostream>
#include <fstream>
#include <cstdlib>

#import <Carbon/Carbon.h>
#import <AudioToolbox/AudioToolbox.h>

#import "synth.h"
#import "module.h"
#import "freqlut.h"
#import "sin.h"
#import "sawtooth.h"

using namespace ::std;


OSStatus audiocallback(void *data,
                       AudioUnitRenderActionFlags *ioActionFlags,
                       const AudioTimeStamp *inTimeStamp,
                       UInt32 inBusNumber,
                       UInt32 inNumberFrames,
                       AudioBufferList *ioData) {
  //cout << "callback!" << inNumberFrames << endl;

  SynthUnit *synth_unit = (SynthUnit *)data;

  SInt16 *buffer = (SInt16 *)ioData->mBuffers[0].mData;
  synth_unit->GetSamples(inNumberFrames, buffer);
  return noErr;
}

// Set up audio playback for Mac
OSStatus SynthMain::setupplayback(SynthUnit *synth_unit) {
  Component component;
  ComponentDescription description;
  OSStatus err = noErr;
  AURenderCallbackStruct callback;

  description.componentType = kAudioUnitType_Output;
  description.componentSubType = kAudioUnitSubType_HALOutput;
  description.componentManufacturer = kAudioUnitManufacturer_Apple;
  description.componentFlags = 0;
  description.componentFlagsMask = 0;
  
  if ((component = FindNextComponent(NULL, &description))) {
    err = OpenAComponent(component, &audioUnit_);
    if (err != noErr) return err;
  }

  UInt32 param = 0;
  err = AudioUnitSetProperty(audioUnit_, kAudioOutputUnitProperty_EnableIO,
      kAudioUnitScope_Input, 1, &param, sizeof(param));
  if (err != noErr) return err;
  param = 1;
  err = AudioUnitSetProperty(audioUnit_, kAudioOutputUnitProperty_EnableIO,
      kAudioUnitScope_Output, 0, &param, sizeof(param));
  if (err != noErr) return err;
  
  AudioDeviceID deviceId = kAudioObjectUnknown;
  UInt32 deviceIdSize = sizeof(deviceId);
  AudioObjectPropertyAddress propertyAddress = {
    kAudioHardwarePropertyDefaultOutputDevice,  // mSelector
    kAudioObjectPropertyScopeGlobal,            // mScope
    kAudioObjectPropertyElementMaster           // mElement
  };
  err = AudioObjectGetPropertyData(kAudioObjectSystemObject,
                                   &propertyAddress,
                                   0,
                                   NULL,
                                   &deviceIdSize,
                                   &deviceId);
  if (err != noErr) return err;
  err = AudioUnitSetProperty(audioUnit_, kAudioOutputUnitProperty_CurrentDevice,
      kAudioUnitScope_Global, 0, &deviceId, sizeof(deviceId));
  if (err != noErr) return err;
  callback.inputProc = audiocallback;
  callback.inputProcRefCon = synth_unit;
  err = AudioUnitSetProperty(audioUnit_, kAudioUnitProperty_SetRenderCallback,
      kAudioUnitScope_Input, 0, &callback, sizeof(callback));
  if (err != noErr) return err;
  
  AudioStreamBasicDescription deviceFormat;
  deviceFormat.mChannelsPerFrame = 1;
  deviceFormat.mSampleRate = 44100.0;
  deviceFormat.mFormatID = kAudioFormatLinearPCM;
  deviceFormat.mFormatFlags = kAudioFormatFlagIsSignedInteger |
    kAudioFormatFlagIsPacked;
  deviceFormat.mBytesPerFrame = 2;
  deviceFormat.mBitsPerChannel = deviceFormat.mBytesPerFrame * 8;
  deviceFormat.mFramesPerPacket = 1;
  deviceFormat.mBytesPerPacket = deviceFormat.mBytesPerFrame;
  err = AudioUnitSetProperty(audioUnit_, kAudioUnitProperty_StreamFormat,
      kAudioUnitScope_Input, 0, &deviceFormat, sizeof(deviceFormat));
  if (err != noErr) return err;
  
  err = AudioUnitInitialize(audioUnit_);
  if (err != noErr) return err;
}

OSStatus SynthMain::startplayback() {
  return AudioOutputUnitStart(audioUnit_);
}

OSStatus SynthMain::stopplayback() {
  return AudioOutputUnitStop(audioUnit_);
}

int SynthMain::Load(const char *filename) {
  uint8_t syx_data[4104];
  ifstream fp_in;
  fp_in.open(filename, ifstream::in);
  if (fp_in.fail()) {
    std::cerr << "error opening file" << std::endl;
    return 1;
  }
  fp_in.read((char *)syx_data, 4104);
  if (fp_in.fail()) {
    std::cerr << "error reading file" << std::endl;
    return 1;
  }
  ring_buffer_.Write(syx_data, 4104);
#if 0
  const uint8_t *data = syx_data + 6 + 128 * patch_num;
  for (int i = 0; i < 6; i++) {
    for (int j = 0; j < 17; j++) {
      if (j == 4 || j == 8) std::cout << " |";
      std::cout << " " << (int)data[i * 17 + j];
    }
    std::cout << std::endl;
  }
  for (int j = 102; j < 118; j++) {
    if (j == 106 || j == 110) std::cout << " |";
    std::cout << " " << (int)data[j];
  }
  std::cout << std::endl;
#endif
}

int SynthMain::SynthInit() {
  double sample_rate = 44100.0;
  synth_unit_ = new SynthUnit(&ring_buffer_);
  SynthUnit::Init(sample_rate);
  if (true) {
    const char *fn = "/Users/raph/dx7/ROM1A.SYX";
    Load(fn);
 }

  OSStatus err = setupplayback(synth_unit_);
  if (err != noErr) {
    cout << err << endl;
    return 1;
  }
  CFStringRef usbname = CFSTR("MPK mini");
  // CFStringRef usbname = CFST("KeyRig 49");
  midi_in_mac_.Init(usbname, &ring_buffer_);
  startplayback();
  return 0;
}

int SynthMain::SynthDone() {
  midi_in_mac_.Done();
  stopplayback();
  delete synth_unit_;
  return 0;
}
