/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.parser_util;

import junit.framework.TestCase;

public class IdentifierUtilJUTest extends TestCase {

	public void testValidId() {
		assertTrue(IdentifierUtil.validId("thingB1"));
	}

	public void testValidId1() {
		assertFalse(IdentifierUtil.validId("ABC"));
	}

	public void testValidId2() {
		assertTrue(IdentifierUtil.validId("x"));
	}

	public void testValidId3() {
		assertTrue(IdentifierUtil.validId("xFFFF"));
	}

	public void testValidId4() {
		assertTrue(IdentifierUtil.validId("_x"));
	}

	public void testValidId5() {
		assertTrue(IdentifierUtil.validId("xFFFF1ddw2s"));
	}

	public void testValidId6() {
		assertFalse(IdentifierUtil.validId("$x"));
	}

	public void testValidId7() {
		assertFalse(IdentifierUtil.validId("7"));
	}

	public void testValidId8() {
		assertFalse(IdentifierUtil.validId("7x"));
	}

	public void testValidId9() {
		assertTrue(IdentifierUtil.validId("x7"));
	}

	public void testValidId10() {
		assertTrue(IdentifierUtil.validId("x7y"));
	}

}
