package nl.nn.adapterframework.doc.testtarget.examples.sequence;

import nl.nn.adapterframework.doc.IbisDoc;

/**
 * We test here that the sequence of groups, config children and attributes is right.
 * It is important that the sequences in @IbisDoc annotations, in the alphabetic order
 * and in the order of appearance are all different. 
 */
public class Master {
	@IbisDoc("10")
	public void setAlpha(IProton child) {
	}

	@IbisDoc("30")
	public void setEpsilon(Opaque child) {
	}

	@IbisDoc("40")
	public void setDelta(INemesis child) {
	}

	@IbisDoc("20")
	public void setBeta(IMnemonic child) {
	}

	@IbisDoc("10")
	public void setClemens(String value) {
	}

	@IbisDoc("40")
	public void setBernhard(String value) {
	}

	@IbisDoc("20")
	public void setArnold(String value) {
	}

	@IbisDoc("30")
	public void setDennis(String value) {
	}
}
