package com.cloudfree;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ib.controller.ApiConnection.ILogger;

public class GenLogger implements ILogger {
	private Log logger = null;
	private int level = INFO;

	public static final int DEBUG = 0;
	public static final int INFO = 1;
	public static final int TRACE = 2;
	public static final int WARN = 3;
	public static final int ERROR = 4;
	public static final int FATAL = 5;

	public static GenLogger DFT_LOGGER = new GenLogger("General", INFO);

	public GenLogger(String what, int l) {

		if (l >= DEBUG && l <= FATAL) {
			level = l;
		}

		logger = LogFactory.getLog(what);
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	public GenLogger(Class what, int l) {

		if (l >= DEBUG && l <= FATAL) {
			level = l;
		}

		logger = LogFactory.getLog(what);
	}

	public void SetLevel(int l) {
		level = l;
	}

	@Override
	public void log(final String str) {
		switch (level) {
		case DEBUG:
			logger.debug(str);
			break;
		case INFO:
			logger.info(str);
			break;
		case TRACE:
			logger.trace(str);
			break;
		case WARN:
			logger.warn(str);
			break;
		case ERROR:
			logger.error(str);
			break;
		case FATAL:
			logger.fatal(str);
			break;
		default:
			logger.info(str);
			break;
		}

	}

	public void log(final String str, int l) {

//		int oldlevel = level;
//		SetLevel(l);
		if (l >= level)	log(str);
//		SetLevel(oldlevel);

	}
}
