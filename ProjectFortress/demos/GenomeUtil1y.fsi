(*******************************************************************************
Copyright 2010 Michael Zody and Sun Microsystems, Inc. 
All rights reserved.

Sun Microsystems, Inc. is the Copyright owner of the Fortress programming language software, and Michael Zody is the developer of the algorithm which this software implements and the Copyright owner of the software implementation of the algorithm, to which Sun Microsystems, Inc. has a perpetual, irrevocable, royalty free right and license to use and distribute. 

Use is subject to license terms accompanying the software.
 ******************************************************************************)

api GenomeUtil1y

import FileSupport.{...}
import List.{...}

colorsFromACGT(strACGT: String): String
colorsToACGT(start: Char, colorstr: String): String

trait ReferenceGenome
  nextChunk(): ()
  currentChunkRange(): CompactFullRange[\ZZ32\]
  getACGT(r: CompactFullRange[\ZZ32\]): String
  getColors(r: CompactFullRange[\ZZ32\]): String
end ReferenceGenome

getReferenceGenomeFromFile(fileName: String, chunkSize: ZZ32, maxReadSize: ZZ32): ReferenceGenome

trait ReadList
  nextRange(r: CompactFullRange[\ZZ32\]): ()
  getReads(): List[\Read\]
end ReadList

getReadListFromFile(fileName: String, maxReadSize: ZZ32): ReadList

trait Read
  header: String
  sequence: String
  name: String
  pos: ZZ32
  length: ZZ32
  seqend: ZZ32
  refColorsSnip: String
  sampleColorsSnip: String
  refACGTSnip: String
  getter asString(): String
end Read

parseOneRead(r: ReadStream, refACGT: String, refColors: String): Maybe[\Read\]

end
