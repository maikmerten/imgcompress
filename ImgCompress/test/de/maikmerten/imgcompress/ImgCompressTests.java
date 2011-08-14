/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.maikmerten.imgcompress;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author maik
 */
public class ImgCompressTests {

	public ImgCompressTests() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}
	// TODO add test methods here.
	// The methods must be annotated with annotation @Test. For example:
	//
	// @Test
	// public void hello() {}

	@Test
	public void testResidueRountrip() {
		
		boolean pass = true;
		

		// check for all predictors and all possible residues if the de- and encoding roundtrip works
		for (int pred = 0; pred <= 255; ++pred) {
			for (int res = -pred; res <= (255 - pred); ++res) {
				int encoded = ImgCompress.encodeResidue(res, pred);
				int decoded = ImgCompress.decodeResidue(encoded, pred);

				if (res != decoded) {
					System.err.println("Residue en-/decoding does not work properly for predictor " + pred + " and residue " + res);
					pass = false;
				}

			}
		}
		
		assertTrue(pass);
		

	}
}
