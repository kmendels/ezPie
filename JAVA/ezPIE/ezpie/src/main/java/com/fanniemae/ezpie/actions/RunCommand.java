/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-16
 * 
 */

public class RunCommand extends Action {
	protected String _workDirectory;
	protected String _commandLine;

	protected String[] _arguments;

	protected Boolean _waitForExit = true;
	protected Boolean _hideConsoleOutput = false;

	protected int _timeout = 0;
	protected int[] _exitCodes = new int[] { 0 };
	protected String _acceptableErrorOutput;
	protected boolean _ignoreErrorCode = false;

	protected String _batchFilename;

	public RunCommand(SessionManager session, Element action) {
		this(session, action, false);
	}

	public RunCommand(SessionManager session, Element action, boolean bIDRequired) {
		super(session, action, bIDRequired);

	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		try {
			_session.setDataTokens(dataTokens);

			if (_action.getNodeName().equals("RunCommand")) {

				_hideConsoleOutput = StringUtilities.toBoolean(optionalAttribute("HideConsoleOutput", null), false);
				_workDirectory = requiredAttribute("WorkDirectory");

				_commandLine = _session.getAttribute(_action, "CommandLine");
				if (StringUtilities.isNullOrEmpty(_commandLine)) {
					throw new RuntimeException("Missing a value for CommandLine on the RunCommand element.");
				}
				_session.addLogMessage("", "CommandLine", (_hideConsoleOutput || _session.lastAttributeSecure()) ? _session.getHiddenMessage() : _commandLine);

				_arguments = parseCommandLine(_commandLine);

			}

			String waitForExit = optionalAttribute("WaitForExit", null);
			String timeout = optionalAttribute("Timeout", "2h");
			Boolean makeBatchFile = StringUtilities.toBoolean(optionalAttribute("MakeBatchFile", null), false);
			if (makeBatchFile) {
				makeBatchFile();
			}

			_waitForExit = StringUtilities.toBoolean(waitForExit, true);
			_timeout = parseTimeout(timeout);

			String sConsoleFilename = FileUtilities.getRandomFilename(_session.getLogPath(), "txt");
			ProcessBuilder pb = new ProcessBuilder(_arguments);
			pb.directory(new File(_workDirectory));
			try {
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, _timeout);
				Date expireTime = calendar.getTime();

				pb.redirectErrorStream(true);
				Process p = pb.start();
				try (InputStream is = p.getInputStream(); InputStreamReader isr = new InputStreamReader(is); FileWriter fw = new FileWriter(sConsoleFilename); BufferedWriter bw = new BufferedWriter(fw);) {
					boolean processTimedOut = false;
					int count = 0;
					char[] buffer = new char[100];
					while (true) {
						if (expireTime.before(new Date())) {
							break;
						}
						if (!p.isAlive()) {
							break;
						} else if (!isr.ready()) {
							p.waitFor(500, TimeUnit.MILLISECONDS);
							continue;
						}
						int charCount = isr.read(buffer);
						if (charCount != -1) {
							bw.write(Arrays.copyOf(buffer, charCount));
							count += charCount;
						}
						count++;
						if (!p.isAlive() && !isr.ready()) {
							break;
						}
					}
					if (_waitForExit) {
						if (p.isAlive() && !p.waitFor(200, TimeUnit.MILLISECONDS)) {
							p.destroy();
							processTimedOut = true;
						}
					}
					bw.flush();
					bw.close();
					if (_hideConsoleOutput) {
						_session.addLogMessage("", "Console Output", _session.getHiddenMessage());
						FileUtilities.deleteFile(sConsoleFilename);
					} else {
						_session.addLogMessage("", "Console Output", String.format("View Console Output (%,d bytes)", count), "file://" + sConsoleFilename);
					}
					if (processTimedOut) {
						// @formatter:off
						throw new RuntimeException("The external command did not return within the timeout period.  It is possible that external command was waiting for some input or it simply became blocked.\n" 
					                               + "Please check for an input prompt before running the command again or use the Timeout attribute to control the timeout length.\n"
								                   + "NOTE: When the command times out the console output could be empty or incomplete due to internal buffering.");
						// @formatter:on
					} else if (!_ignoreErrorCode && ArrayUtilities.indexOf(_exitCodes, p.exitValue()) == -1) {
						throw new RuntimeException(String.format("External command returned an error code of %d.  View console output for error details.", p.exitValue()));
					}
				} catch (InterruptedException ex) {
					_session.addErrorMessage(ex);
					throw new RuntimeException("Interrupted exception error while running external command.", ex);
				}
				_session.addLogMessage("", "Exit Code", p.exitValue() + "");
			} catch (IOException ex) {
				throw new RuntimeException("Error while running external command.", ex);
			}
		} finally {
			_session.clearDataTokens();
			if (FileUtilities.isValidFile(_batchFilename)) {
				try {
					FileUtilities.deleteFile(_batchFilename);
				} catch (Exception e) {
					_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "Delete Batch", "Could not delete batch file. " + e.getMessage());
				}
			}
		}
		return null;
	}

	protected String[] parseCommandLine(String commandLine) {
		if (commandLine == null)
			return null;

		List<String> aArgs = new ArrayList<String>();
		boolean bInQuotes = false;
		int iLen = commandLine.length();
		int iStart = 0;
		for (int i = 0; i < iLen; i++) {
			char c = commandLine.charAt(i);
			switch (c) {
			case '"':
				if (bInQuotes) {
					if ((i - 1 > 0) && (commandLine.charAt(i - 1) == '\\'))
						continue;
					aArgs.add(commandLine.substring(iStart, i + 1));
					bInQuotes = false;
					iStart = i + 1;
				} else {
					bInQuotes = true;
				}
				break;
			case ' ':
				if (bInQuotes)
					continue;
				if (iStart == i) {
					iStart += 1;
					continue;
				}
				if (iStart >= iLen)
					break;
				aArgs.add(commandLine.substring(iStart, i));
				iStart = i + 1;
				break;
			}
		}
		if (iStart < iLen)
			aArgs.add(commandLine.substring(iStart));

		return aArgs.toArray(new String[0]);
	}

	protected int parseTimeout(String value) {
		if (StringUtilities.isNullOrEmpty(value))
			return -1;

		char cUnits = 's';

		int iStart = 0;
		int iSeconds = 0;
		int iPosition = 0;
		int iCurrentValue = 0;

		value = value.toLowerCase();
		String[] aNumbers = value.split("d|h|m|s");
		for (int i = 0; i < aNumbers.length; i++) {
			if (StringUtilities.isNullOrEmpty(aNumbers[i]))
				continue;

			iPosition = value.indexOf(aNumbers[i], iStart) + aNumbers[i].length();
			iStart = iPosition + 1;
			cUnits = (iPosition < value.length()) ? value.charAt(iPosition) : 's';
			iCurrentValue = StringUtilities.toInteger(aNumbers[i], 0);

			switch (cUnits) {
			case 'd':
				iSeconds += iCurrentValue * 86400;
				break;
			case 'h':
				iSeconds += iCurrentValue * 3600;
				break;
			case 'm':
				iSeconds += iCurrentValue * 60;
				break;
			case 's':
				iSeconds += iCurrentValue;
				break;
			}
		}
		return (iSeconds < 1) ? -1 : iSeconds;
	}

	protected void makeBatchFile() {
		_batchFilename = FileUtilities.writeRandomFile(_session.getStagingPath(), "bat", ArrayUtilities.toCommandLine(_arguments));
		_session.addLogMessage("", "Created Batch File", _batchFilename.replace(_session.getApplicationPath(), ""));
		_arguments = new String[] { _batchFilename };
	}
}
