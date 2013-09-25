package fr.les_enry.bataillenavale;

class BatailleNavaleException extends Exception {

	/**
	 * Serialisation UID.
	 */
	private static final long serialVersionUID = 7144980425386300609L;

	/**
	 * @param detailMessage Exception explanation.
	 */
	BatailleNavaleException(String detailMessage) {
		super(detailMessage);
	}

}

class BadPlacementException extends Exception {

	/**
	 * Serialisation UID.
	 */
	private static final long serialVersionUID = -2164372343208044466L;

	/**
	 * @param detailMessage Exception explanation.
	 */
	BadPlacementException(String detailMessage) {
		super(detailMessage);
	}

}


class CantShootHereException extends Exception {

	/**
	 * Serialisation UID.
	 */
	private static final long serialVersionUID = -2994998465632405433L;

	/**
	 * @param detailMessage Exception explanation.
	 */
	CantShootHereException(String detailMessage) {
		super(detailMessage);
	}

}
