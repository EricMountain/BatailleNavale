package fr.les_enry.bataillenavale;

class AlreadyPlayedException extends BatailleNavaleException {

	/**
	 * Serialisation UID.
	 */
	private static final long serialVersionUID = -4091840682635317236L;

	/**
	 * @param detailMessage
	 */
	AlreadyPlayedException(String detailMessage) {
		super(detailMessage);
	}
	
}

class AlreadyPlayedShotException extends AlreadyPlayedException {


	/**
	 * Serialisation UID.
	 */
	private static final long serialVersionUID = 7764680315092797439L;

	/**
	 * @param detailMessage
	 */
	AlreadyPlayedShotException(String detailMessage) {
		super(detailMessage);
	}
	
}
