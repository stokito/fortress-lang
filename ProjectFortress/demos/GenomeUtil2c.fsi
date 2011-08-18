(*******************************************************************************
Copyright 2010 Michael Zody and Sun Microsystems, Inc. 
All rights reserved.

Sun Microsystems, Inc. is the Copyright owner of the Fortress programming language software,
and Michael Zody is the developer of the algorithm which this software implements and the
Copyright owner of the software implementation of the algorithm, to which Sun Microsystems, Inc.
has a perpetual, irrevocable, royalty free right and license to use and distribute. 

Use is subject to license terms accompanying the software.
 ******************************************************************************)

api GenomeUtil2c

import FileSupport.{...}
import List.{...}

colorsFromACGT(strACGT: String): String
colorsToACGT(start: Char, colorstr: String): String

trait ReferenceGenome extends { SequentialGenerator[\ReferenceGenomeChunk\] } end

trait ReferenceGenomeChunk comprises {...}
  currentChunkRange(): CompactFullRange[\ZZ32\]
  getACGT(r: CompactFullRange[\ZZ32\]): String
  getColors(r: CompactFullRange[\ZZ32\]): String
end ReferenceGenomeChunk

getReferenceGenomeFromFile(fileName: String, chunkSize: ZZ32, maxReadSize: ZZ32): ReferenceGenome

trait ReadList
  getReads(refChunk: ReferenceGenomeChunk): List[\Read\]
end ReadList

getReadListFromFile(fileName: String, maxReadSize: ZZ32): ReadList

trait Read comprises {...}
  header: String
  sampleACGT: String
  name: String
  pos: ZZ32
  length: ZZ32
  seqend: ZZ32
  getter asString(): String
  getter refACGT(): String
  getter refColors(): String
  getter sampleColors(): String
end Read

end
