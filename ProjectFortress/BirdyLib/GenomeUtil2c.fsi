(*******************************************************************************
Copyright 2012 Michael Zody and Oracle. 
All rights reserved.

Oracle is the Copyright owner of the Fortress programming language software,
and Michael Zody is the developer of the algorithm which this software implements and the
Copyright owner of the software implementation of the algorithm, to which Oracle
has a perpetual, irrevocable, royalty free right and license to use and distribute. 

Use is subject to license terms accompanying the software.
 ******************************************************************************)

api GenomeUtil2c

import List.{...}
import Util.{...}
import GeneratorLibrary.{DefaultSequentialGeneratorImplementation}

colorsFromACGT(strACGT: String): String
colorsToACGT(start: Character, colorstr: String): String

trait ReferenceGenome extends { DefaultSequentialGeneratorImplementation[\ReferenceGenomeChunk\] } end

object ReferenceGenomeChunk(start: ZZ32, length: ZZ32, buffer: String, padLength: ZZ32)
  getter range(): Range
  getACGT(r: Range): String
  getColors(r: Range): String
end ReferenceGenomeChunk

getReferenceGenomeFromFile(fileName: String, chunkSize: ZZ32, maxReadSize: ZZ32): ReferenceGenome

trait ReadList
  abstract getReads(refChunk: ReferenceGenomeChunk): List[\Read\]
end ReadList

getReadListFromFile(fileName: String, maxReadSize: ZZ32): ReadList

object Read(header: String, sequence: String, name: String, pos: ZZ32, length: ZZ32, seqend: ZZ32,
            negativeOrientation: Boolean, refChunk: ReferenceGenomeChunk)
  getter asString(): String
  getter refACGT(): String
  getter refColors(): String
  getter sampleColors(): String
end Read

end
