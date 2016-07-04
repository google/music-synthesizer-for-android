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

#import "SynthMain.h"

#import "SynthAppDelegate.h"

@implementation SynthAppDelegate

@synthesize window;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
  synthMain.SynthInit();
}

- (void)openDocument:(id)sender {
  NSLog(@"openDocument");
  NSOpenPanel *openPanel;

  openPanel = [NSOpenPanel openPanel];
  if (NSOKButton == [openPanel runModal]) {
    NSArray *selectedPaths = [openPanel filenames];
    NSEnumerator *enumerator = [selectedPaths objectEnumerator];
    NSString *currentPath;
    while (nil != (currentPath = [enumerator nextObject])) {
      const char *filename = [currentPath UTF8String];
      synthMain.Load(filename);
    }
  }
}

- (BOOL)application:(NSApplication *)theApplication openFile:(NSString *)filename {
  NSLog(@"open");
}

@end
