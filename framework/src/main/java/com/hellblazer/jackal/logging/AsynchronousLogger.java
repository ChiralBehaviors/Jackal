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

import java.util.concurrent.Executor;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class AsynchronousLogger implements Log {
    private final Logger log;
    private final Executor executor;

    public AsynchronousLogger(Logger log, Executor executor) {
        this.log = log;
        this.executor = executor;
    }

    @Override
    public void config(final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.config(msg);
            }
        });
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.entering(sourceClass, sourceMethod);
            }
        });
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod,
                         final Object param1) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.entering(sourceClass, sourceMethod, param1);
            }
        });
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod,
                         final Object[] params) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.entering(sourceClass, sourceMethod, params);
            }
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AsynchronousLogger other = (AsynchronousLogger) obj;
        if (log == null) {
            if (other.log != null) {
                return false;
            }
        } else if (!log.equals(other.log)) {
            return false;
        }
        return true;
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.exiting(sourceClass, sourceMethod);
            }
        });
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod,
                        final Object result) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.exiting(sourceClass, sourceMethod, result);
            }
        });
    }

    @Override
    public void fine(final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.fine(msg);
            }
        });
    }

    @Override
    public void finer(final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.finer(msg);
            }
        });
    }

    @Override
    public void finest(final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.finest(msg);
            }
        });
    }

    @Override
    public Filter getFilter() {
        return log.getFilter();
    }

    @Override
    public Level getLevel() {
        return log.getLevel();
    }

    @Override
    public String getName() {
        return log.getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (log == null ? 0 : log.hashCode());
        return result;
    }

    @Override
    public void info(final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.info(msg);
            }
        });
    }

    @Override
    public boolean isLoggable(final Level level) {
        return log.isLoggable(level);
    }

    @Override
    public void log(final Level level, final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.log(level, msg);
            }
        });
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.log(level, msg, param1);
            }
        });
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.log(level, msg, params);
            }
        });
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.log(level, msg, thrown);
            }
        });
    }

    @Override
    public void log(final LogRecord record) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.log(record);
            }
        });
    }

    @Override
    public void logp(final Level level, final String sourceClass,
                     final String sourceMethod, final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.logp(level, sourceClass, sourceMethod, msg);
            }
        });
    }

    @Override
    public void logp(final Level level, final String sourceClass,
                     final String sourceMethod, final String msg,
                     final Object param1) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.logp(level, sourceClass, sourceMethod, msg, param1);
            }
        });
    }

    @Override
    public void logp(final Level level, final String sourceClass,
                     final String sourceMethod, final String msg,
                     final Object[] params) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.logp(level, sourceClass, sourceMethod, msg, params);
            }
        });
    }

    @Override
    public void logp(final Level level, final String sourceClass,
                     final String sourceMethod, final String msg,
                     final Throwable thrown) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.logp(level, sourceClass, sourceMethod, msg, thrown);
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass,
                      final String sourceMethod, final String bundleName,
                      final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.logrb(level, sourceClass, sourceMethod, bundleName, msg);
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass,
                      final String sourceMethod, final String bundleName,
                      final String msg, final Object param1) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.logrb(level, sourceClass, sourceMethod, bundleName, msg,
                          param1);
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass,
                      final String sourceMethod, final String bundleName,
                      final String msg, final Object[] params) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.logrb(level, sourceClass, sourceMethod, bundleName, msg,
                          params);
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass,
                      final String sourceMethod, final String bundleName,
                      final String msg, final Throwable thrown) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.logrb(level, sourceClass, sourceMethod, bundleName, msg,
                          thrown);
            }
        });
    }

    @Override
    public void setFilter(Filter newFilter) throws SecurityException {
        log.setFilter(newFilter);
    }

    @Override
    public void setLevel(Level newLevel) throws SecurityException {
        log.setLevel(newLevel);
    }

    @Override
    public void severe(final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.severe(msg);
            }
        });
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod,
                         final Throwable thrown) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.throwing(sourceClass, sourceMethod, thrown);
            }
        });
    }

    @Override
    public String toString() {
        return "Asynchronous wrapping of [" + log.toString() + "]";
    }

    @Override
    public void warning(final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.warning(msg);
            }
        });
    }
}
