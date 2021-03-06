// Copyright 2011-2013 the original author or authors.
//
// @package com.jetdrone.vertx.yoke.middleware
package com.jetdrone.vertx.yoke.middleware;

import com.jetdrone.vertx.yoke.Middleware;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpVersion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Logger extends Middleware {

    private final SimpleDateFormat ISODATE;

    public enum Format {
        DEFAULT,
        SHORT,
        TINY
    }

    final boolean immediate;
    final Format format;

    public Logger(boolean immediate, Format format) {
        this.immediate = immediate;
        this.format = format;

        ISODATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS zzz");
        ISODATE.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public Logger(Format format) {
        this(false, format);
    }

    public Logger() {
        this(false, Format.DEFAULT);
    }

    private String getVersionString(HttpVersion version) {
        switch (version) {
            case HTTP_1_1:
                return "HTTP/1.1";
            case HTTP_1_0:
                return "HTTP/1.0";
        }
        return null;
    }

    private void log(YokeRequest request, long start) {
        int contentLength = 0;
        if (immediate) {
            Object obj = request.getHeader("content-length");
            if (obj != null) {
                contentLength = Integer.parseInt(obj.toString());
            }
        } else {
            Object obj = request.response().getHeader("content-length");
            if (obj != null) {
                contentLength = Integer.parseInt(obj.toString());
            }
        }

        int status = request.response().getStatusCode();
        String message = null;

        switch (format) {
            case DEFAULT:
                Object referrer = request.getHeader("referrer", "");
                Object userAgent = request.getHeader("user-agent", "");

                message = String.format("%s - - [%s] \"%s %s %s\" %d %d \"%s\" \"%s\"",
                        request.remoteAddress().getHostString(),
                        ISODATE.format(new Date(start)),
                        request.method(),
                        request.uri(),
                        getVersionString(request.version()),
                        status,
                        contentLength,
                        referrer,
                        userAgent);
                break;
            case SHORT:
                message = String.format("%s - %s %s %s %d %d - %d ms",
                        request.remoteAddress().getHostString(),
                        request.method(),
                        request.uri(),
                        getVersionString(request.version()),
                        status,
                        contentLength,
                        (System.currentTimeMillis() - start));
                break;
            case TINY:
                message = String.format("%s %s %d %d - %d ms",
                        request.method(),
                        request.uri(),
                        request.response().getStatusCode(),
                        contentLength,
                        (System.currentTimeMillis() - start));
                break;
        }

        if (status >= 500) {
            logger.fatal(message);
        } else if (status >= 400) {
            logger.error(message);
        } else if (status >= 300) {
            logger.warn(message);
        } else {
            logger.info(message);
        }
    }

    @Override
    public void handle(final YokeRequest request, Handler<Object> next) {
        final long start = System.currentTimeMillis();

        if (immediate) {
            log(request, start);
        } else {
            request.response().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    log(request, start);
                }
            });
        }

        next.handle(null);
    }
}
