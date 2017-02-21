/**
 * Copyright (c) 2015, biezhi 王爵 (biezhi.me@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blade.mvc;

import com.blade.Blade;
import com.blade.Const;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Blade Asynchronous DispatcherServlet
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.7.1-alpha
 */
public class AsyncDispatcherServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncDispatcherServlet.class);

    private static final long serialVersionUID = -2607425162473178733L;

    private static ThreadPoolExecutor executor;
    private Blade blade;
    private DispatcherHandler dispatcherHandler;
    private int asyncContextTimeout;

    public AsyncDispatcherServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        blade = Blade.$();
        this.asyncContextTimeout = blade.config().getInt("server.async-ctx-timeout", 10 * 1000);
        this.dispatcherHandler = new DispatcherHandler(config.getServletContext(), blade.routers());

        executor = new ThreadPoolExecutor(100, 200, 50000L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100));
        LOGGER.info("init worker thread pool.");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.service(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.service(req, resp);
    }

    @Override
    protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

        LOGGER.debug("AsyncLongRunningServlet Start::Name={} :: ID={}", Thread.currentThread().getName(), Thread.currentThread().getId());

        httpRequest.setCharacterEncoding(blade.encoding());
        httpResponse.setCharacterEncoding(blade.encoding());
        httpResponse.setHeader("X-Powered-By", "Blade(" + Const.VERSION + ")");
        httpRequest.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);

        AsyncContext asyncContext = httpRequest.startAsync();
        asyncContext.addListener(new BladeAsyncListener());
        asyncContext.setTimeout(asyncContextTimeout);
        executor.execute(new AsyncRequestProcessor(asyncContext, dispatcherHandler));
    }

    @Override
    public void destroy() {
        super.destroy();
        if (null != executor) {
            executor.shutdown();
            LOGGER.info("shutdown worker thread pool.");
        }
    }

}