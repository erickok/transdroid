/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.daemon;


/**
 * An exception thrown when an error occurs inside a server daemon adapter.
 * The error message is from a resource string ID, since this can be 
 * translated. An alternative message is given to use as logging output not 
 * visible to the user.
 * 
 * @author erickok
 *
 */
public class DaemonException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private ExceptionType internalException;

	public enum ExceptionType {
		MethodUnsupported,
		ConnectionError,
		UnexpectedResponse,
		ParsingFailed,
		AuthenticationFailure,
		NotConnected,
		FileAccessError;
	}
	
	public DaemonException(ExceptionType internalException, String message) {
		super(message);
		this.internalException = internalException;
	}

	public ExceptionType getType() {
		return internalException;
	}
	
	@Override
	public String toString() {
		return internalException.toString() + " exception: " + getMessage();
	}

}
