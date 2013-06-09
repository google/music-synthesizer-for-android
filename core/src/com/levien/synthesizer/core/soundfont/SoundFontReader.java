/*
 * Copyright 2011 Google Inc.
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

package com.levien.synthesizer.core.soundfont;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

//import com.levien.synthesizer.core.model.sample.Proto;
import com.levien.synthesizer.core.wave.RiffInputStream;

/**
 * Reads a SoundFont file from a stream and provides access its data.
 */
public class SoundFontReader {
  /**
   * Reads the .sf2 file from a stream and stores the data in a buffer.
   * @param input - the stream to read from.
   * @throws IOExpection - on a malformed or unsupported file format.
   */
  public SoundFontReader(InputStream in) throws IOException {
    Logger logger = Logger.getLogger(getClass().getName());
    logger.info("Loading SoundFont file.");

    RiffInputStream input = new RiffInputStream(in);
    
    input.checkBytes("RIFF");
    long riffSize = input.readDWord();
    long riffRemaining = riffSize;

    input.checkBytes("sfbk");
    riffRemaining -= 4;

    // All of the sample data, scaled from -1 to 1.
    double[] samples = null;

    while (riffRemaining > 0) {
      input.checkBytes("LIST");
      long listSize = input.readDWord();
      riffRemaining -= 8;
      riffRemaining -= listSize;
      String chunk = input.readString(4);
      long listRemaining = listSize;
      listRemaining -= 4;
      if (chunk.equals("INFO")) {
        // Mostly worthless info headers.
        while (listRemaining > 0) {
          String subchunk = input.readString(4);
          int subchunkSize = (int)input.readDWord();
          listRemaining -= 8;
          listRemaining -= subchunkSize;
          if (subchunk.equals("ifil")) {
            if (subchunkSize != 4) {
              throw new IOException("ifil size != 4.");
            }
            majorVersion_ = input.readWord();
            minorVersion_ = input.readWord();
          } else if (subchunk.equals("isng")) {
            engine_ = input.readString(subchunkSize);
          } else if (subchunk.equals("INAM")) {
            name_ = input.readString(subchunkSize);
          } else if (subchunk.equals("irom")) {
            rom_ = input.readString(subchunkSize);
          } else if (subchunk.equals("iver")) {
            if (subchunkSize != 4) {
              throw new IOException("iver size != 4.");
            }
            romMajorVersion_ = input.readWord();
            romMinorVersion_ = input.readWord();
          } else if (subchunk.equals("ICRD")) {
            creationDate_ = input.readString(subchunkSize);
          } else if (subchunk.equals("IENG")) {
            engineer_ = input.readString(subchunkSize);
          } else if (subchunk.equals("IPRD")) {
            product_ = input.readString(subchunkSize);
          } else if (subchunk.equals("ICOP")) {
            copyright_ = input.readString(subchunkSize);
          } else if (subchunk.equals("ICMT")) {
            comment_ = input.readString(subchunkSize);
          } else if (subchunk.equals("ISFT")) {
            software_ = input.readString(subchunkSize);
          } else {
            logger.info("Skipping unknown INFO subchunk type " + subchunk + ".");
            input.skipBytes(subchunkSize);
          }
        }
        System.out.println("Read SoundFont INFO: \n" + this.toString());
      } else if (chunk.equals("sdta")) {
        // The actual sample data.
        while (listRemaining > 0) {
          String subchunk = input.readString(4);
          long subchunkSize = input.readDWord();
          listRemaining -= 8;
          listRemaining -= subchunkSize;
          if (subchunk.equals("smpl")) {
            int words = (int)(subchunkSize / 2);
            logger.info("Reading " + words + " samples.");
            samples = new double[words];
            for (int i = 0; i < words; ++i) {
              samples[i] = input.readShort() / 32768.0;
            }
          } else {
            logger.info("Skipping unknown sdta subchunk type " + subchunk + ".");
            input.skipBytes(subchunkSize);
          }
        }
      } else if (chunk.equals("pdta")) {
        // This is the so-called HYDRA data structure, with 9 parts.

        ArrayList<Bag> presetBagList = new ArrayList<Bag>();
        ArrayList<Modulator> presetModulatorList = new ArrayList<Modulator>();
        ArrayList<Generator> presetGeneratorList = new ArrayList<Generator>();
        ArrayList<Instrument> instrumentList = new ArrayList<Instrument>();
        ArrayList<Bag> instrumentBagList = new ArrayList<Bag>();
        ArrayList<Modulator> instrumentModulatorList = new ArrayList<Modulator>();
        ArrayList<Generator> instrumentGeneratorList = new ArrayList<Generator>();
        ArrayList<Sample> sampleHeaderList = new ArrayList<Sample>();

        // Preset data is the metadata about the samples.
        while (listRemaining > 0) {
          String subchunk = input.readString(4);
          long subchunkSize = input.readDWord();
          listRemaining -= 8;
          listRemaining -= subchunkSize;
          byte[] subchunkData = input.readBytes((int)subchunkSize);
          ByteArrayInputStream subchunkInput = new ByteArrayInputStream(subchunkData);
          if (subchunk.equals("phdr")) {
            Preset previous = null;
            while (subchunkInput.available() > 0) {
              Preset preset = new Preset(new RiffInputStream(subchunkInput));
              logger.info("Found preset: " + preset.getName());
              if (previous != null) {
                previous.setBagEnd(preset.getBagStart());
                presets_.add(previous);
              }
              previous = preset;
            }
          } else if (subchunk.equals("pbag")) {
            Bag previous = null;
            while (subchunkInput.available() > 0) {
              Bag presetBag = new Bag(new RiffInputStream(subchunkInput));
              if (previous != null) {
                previous.setGeneratorEnd(presetBag.getGeneratorStart());
                previous.setModulatorEnd(presetBag.getModulatorStart());
                presetBagList.add(previous);
              }
              previous = presetBag;
            }
          } else if (subchunk.equals("pmod")) {
            while (subchunkInput.available() > 0) {
              Modulator presetModulator = new Modulator(new RiffInputStream(subchunkInput));
              if (subchunkInput.available() > 0) {
                presetModulatorList.add(presetModulator);
              }
            }
          } else if (subchunk.equals("pgen")) {
            while (subchunkInput.available() > 0) {
              Generator generator = new Generator(new RiffInputStream(subchunkInput));
              if (subchunkInput.available() > 0) {
                presetGeneratorList.add(generator);
              }
            }
          } else if (subchunk.equals("inst")) {
            Instrument previous = null;
            while (subchunkInput.available() > 0) {
              Instrument instrument = new Instrument(new RiffInputStream(subchunkInput));
              logger.info("Found instrument: " + instrument.getName());
              if (previous != null) {
                previous.setBagEnd(instrument.getBagStart());
                instrumentList.add(previous);
              }
              previous = instrument;
            }
          } else if (subchunk.equals("ibag")) {
            Bag previous = null;
            while (subchunkInput.available() > 0) {
              Bag instrumentBag = new Bag(new RiffInputStream(subchunkInput));
              if (previous != null) {
                previous.setGeneratorEnd(instrumentBag.getGeneratorStart());
                previous.setModulatorEnd(instrumentBag.getModulatorStart());
                instrumentBagList.add(previous);
              }
              previous = instrumentBag;
            }
          } else if (subchunk.equals("imod")) {
            while (subchunkInput.available() > 0) {
              Modulator instrumentModulator = new Modulator(new RiffInputStream(subchunkInput));
              if (subchunkInput.available() > 0) {
                instrumentModulatorList.add(instrumentModulator);
              }
            }
          } else if (subchunk.equals("igen")) {
            while (subchunkInput.available() > 0) {
              Generator generator = new Generator(new RiffInputStream(subchunkInput));
              if (subchunkInput.available() > 0) {
                instrumentGeneratorList.add(generator);
              }
            }
          } else if (subchunk.equals("shdr")) {
            while (subchunkInput.available() > 0) {
              Sample sampleHeader = new Sample(new RiffInputStream(subchunkInput),
                                               samples);
              if (subchunkInput.available() > 0) {
                sampleHeaderList.add(sampleHeader);
              }
            }
          } else {
            logger.info("Skipping unknown pdta subchunk type " + subchunk + ".");
          }
        }

        // Now that we've read in the HYDRA, we have to backtrack through it to convert the _bags_
        // to _zones_.

        for (Preset preset : presets_) {
          // There might be a "global" zone for the preset that applies to all the other zones.
          Zone globalZone = null;
          for (int bag = preset.getBagStart(); bag < preset.getBagEnd(); ++bag) {
            // Start with the global zone, if it exists.
            Zone zone = (globalZone != null) ? globalZone.copy() : new Zone();
            for (int gen = presetBagList.get(bag).getGeneratorStart();
                 gen < presetBagList.get(bag).getGeneratorEnd();
                 ++gen) {
              zone.addPresetGenerator(presetGeneratorList.get(gen), instrumentList);
            }
            for (int mod = presetBagList.get(bag).getModulatorStart();
                 mod < presetBagList.get(bag).getModulatorEnd();
                 ++mod) {
              zone.addModulator(presetModulatorList.get(mod));
            }
            // This might be the global zone for the preset.
            if (zone.getInstrument() == null) {
              if (globalZone == null) {
                // It is the global zone, so set it.
                globalZone = zone;
              }
              // Any zone missing an instrument is either the global zone or should be ignored.
              continue;
            }
            // Okay, it's not the global zone.
            preset.addZone(zone);
          }
        }

        for (Instrument instrument : instrumentList) {
          // There might be a "global" zone for the preset that applies to all the other zones.
          Zone globalZone = null;
          for (int bag = instrument.getBagStart(); bag < instrument.getBagEnd(); ++bag) {
            // Start with the global zone, if it exists.
            Zone zone = (globalZone != null) ? globalZone.copy() : new Zone();
            for (int gen = instrumentBagList.get(bag).getGeneratorStart();
                 gen < instrumentBagList.get(bag).getGeneratorEnd();
                 ++gen) {
              zone.addInstrumentGenerator(instrumentGeneratorList.get(gen), sampleHeaderList);
            }
            for (int mod = instrumentBagList.get(bag).getModulatorStart();
                 mod < instrumentBagList.get(bag).getModulatorEnd();
                 ++mod) {
              zone.addModulator(instrumentModulatorList.get(mod));
            }
            // This might be the global zone for the preset.
            if (zone.getSample() == null) {
              if (globalZone == null) {
                // It is the global zone, so set it.
                globalZone = zone;
              }
              // Any zone missing an instrument is either the global zone or should be ignored.
              continue;
            }
            // Okay, it's not the global zone.
            instrument.addZone(zone);
          }
        }

        // TODO(klimt): So, actually some of the generator values for an instrument should be added
        // to the generator values for the preset zone it's in, but an instrument could be in
        // multiple different preset zones, so there's no way to store that here.  So I need to make
        // a function to add two zones, and call it whenever the instrument zone is used.  Grrr.
      } else {
        logger.info("Skipping unknown sfbk LIST type " + chunk + ".");
        input.skipBytes(listRemaining);
      }
    }
    input.close();
  }

  public List<Preset> getPresets() {
    return presets_;
  }

  public String toString() {
    return
        "version:     " + majorVersion_ + "." + minorVersion_ + "\n" +
        "engine:      " + engine_ + "\n" +
        "name:        " + name_ + "\n" +
        "rom:         " + rom_ + "\n" +
        "rom version: " + romMajorVersion_ + "." + romMinorVersion_ + "\n" +
        "created:     " + creationDate_ + "\n" +
        "engineer:    " + engineer_ + "\n" +
        "product:     " + product_ + "\n" +
        "copyright:   " + copyright_ + "\n" +
        "comment:     " + comment_ + "\n" +
        "software:    " + software_ + "\n";
  }

  private int majorVersion_;
  private int minorVersion_;
  private String engine_;
  private String name_;
  private String rom_;
  private int romMajorVersion_;
  private int romMinorVersion_;
  private String creationDate_;
  private String engineer_;
  private String product_;
  private String copyright_;
  private String comment_;
  private String software_;
  private ArrayList<Preset> presets_ = new ArrayList<Preset>();
}
