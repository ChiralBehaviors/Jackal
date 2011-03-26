/** (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.jackal.logging;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public interface Log {
 

    /**
     * Set a filter to control output on this Logger.
     * <P>
     * After passing the initial "level" check, the Logger will
     * call this Filter to check if a log record should really
     * be published.
     *
     * @param   newFilter  a filter object (may be null)
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public abstract void setFilter(Filter newFilter) throws SecurityException;

    /**
     * Get the current filter for this Logger.
     *
     * @return  a filter object (may be null)
     */
    public abstract Filter getFilter();

    /**
     * Log a LogRecord.
     * <p>
     * All the other logging methods in this class call through
     * this method to actually perform any logging.  Subclasses can
     * override this single method to capture all log activity.
     *
     * @param record the LogRecord to be published
     */
    public abstract void log(LogRecord record);

    /**
     * Log a message, with no arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void log(Level level, String msg);

    /**
     * Log a message, with one object parameter.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   param1  parameter to the message
     */
    public abstract void log(Level level, String msg, Object param1);

    /**
     * Log a message, with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  array of parameters to the message
     */
    public abstract void log(Level level, String msg, Object params[]);

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    public abstract void log(Level level, String msg, Throwable thrown);

    /**
     * Log a message, specifying source class and method,
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void logp(Level level, String sourceClass,
                              String sourceMethod, String msg);

    /**
     * Log a message, specifying source class and method,
     * with a single object parameter to the log message.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg      The string message (or a key in the message catalog)
     * @param   param1    Parameter to the log message.
     */
    public abstract void logp(Level level, String sourceClass,
                              String sourceMethod, String msg, Object param1);

    /**
     * Log a message, specifying source class and method,
     * with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  Array of parameters to the message
     */
    public abstract void logp(Level level, String sourceClass,
                              String sourceMethod, String msg, Object params[]);

    /**
     * Log a message, specifying source class and method,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    public abstract void logp(Level level, String sourceClass,
                              String sourceMethod, String msg, Throwable thrown);

    /**
     * Log a message, specifying source class, method, and resource bundle name
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg, 
     *                         can be null
     * @param   msg     The string message (or a key in the message catalog)
     */

    public abstract void logrb(Level level, String sourceClass,
                               String sourceMethod, String bundleName,
                               String msg);

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with a single object parameter to the log message.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null
     * @param   msg      The string message (or a key in the message catalog)
     * @param   param1    Parameter to the log message.
     */
    public abstract void logrb(Level level, String sourceClass,
                               String sourceMethod, String bundleName,
                               String msg, Object param1);

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null.
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  Array of parameters to the message
     */
    public abstract void logrb(Level level, String sourceClass,
                               String sourceMethod, String bundleName,
                               String msg, Object params[]);

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    public abstract void logrb(Level level, String sourceClass,
                               String sourceMethod, String bundleName,
                               String msg, Throwable thrown);

    /**
     * Log a method entry.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     */
    public abstract void entering(String sourceClass, String sourceMethod);

    /**
     * Log a method entry, with one parameter.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY {0}", log level
     * FINER, and the given sourceMethod, sourceClass, and parameter
     * is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   param1         parameter to the method being entered
     */
    public abstract void entering(String sourceClass, String sourceMethod,
                                  Object param1);

    /**
     * Log a method entry, with an array of parameters.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY" (followed by a 
     * format {N} indicator for each entry in the parameter array), 
     * log level FINER, and the given sourceMethod, sourceClass, and 
     * parameters is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   params         array of parameters to the method being entered
     */
    public abstract void entering(String sourceClass, String sourceMethod,
                                  Object params[]);

    /**
     * Log a method return.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method 
     */
    public abstract void exiting(String sourceClass, String sourceMethod);

    /**
     * Log a method return, with result object.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN {0}", log level
     * FINER, and the gives sourceMethod, sourceClass, and result
     * object is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method 
     * @param   result  Object that is being returned
     */
    public abstract void exiting(String sourceClass, String sourceMethod,
                                 Object result);

    /**
     * Log throwing an exception.
     * <p>
     * This is a convenience method to log that a method is
     * terminating by throwing an exception.  The logging is done 
     * using the FINER level.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.  The
     * LogRecord's message is set to "THROW".
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod  name of the method.
     * @param   thrown  The Throwable that is being thrown.
     */
    public abstract void throwing(String sourceClass, String sourceMethod,
                                  Throwable thrown);

    /**
     * Log a SEVERE message.
     * <p>
     * If the logger is currently enabled for the SEVERE message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void severe(String msg);

    /**
     * Log a WARNING message.
     * <p>
     * If the logger is currently enabled for the WARNING message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void warning(String msg);

    /**
     * Log an INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void info(String msg);

    /**
     * Log a CONFIG message.
     * <p>
     * If the logger is currently enabled for the CONFIG message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void config(String msg);

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void fine(String msg);

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void finer(String msg);

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public abstract void finest(String msg);

    /**
     * Set the log level specifying which message levels will be
     * logged by this logger.  Message levels lower than this
     * value will be discarded.  The level value Level.OFF
     * can be used to turn off logging.
     * <p>
     * If the new level is null, it means that this node should
     * inherit its level from its nearest ancestor with a specific
     * (non-null) level value.
     * 
     * @param newLevel   the new value for the log level (may be null)
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public abstract void setLevel(Level newLevel) throws SecurityException;

    /**
     * Get the log Level that has been specified for this Logger.
     * The result may be null, which means that this logger's
     * effective level will be inherited from its parent.
     *
     * @return  this Logger's level
     */
    public abstract Level getLevel();

    /**
     * Check if a message of the given level would actually be logged
     * by this logger.  This check is based on the Loggers effective level,
     * which may be inherited from its parent.
     *
     * @param   level   a message logging level
     * @return  true if the given message level is currently being logged.
     */
    public abstract boolean isLoggable(Level level);

    /**
     * Get the name for this logger.
     * @return logger name.  Will be null for anonymous Loggers.
     */
    public abstract String getName();

}