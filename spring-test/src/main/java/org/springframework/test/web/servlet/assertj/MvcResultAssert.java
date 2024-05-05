/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.assertj;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import jakarta.servlet.http.Cookie;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.http.MediaTypeAssert;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.servlet.ModelAndView;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied
 * to {@link MvcResult}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.2
 */
public class MvcResultAssert extends AbstractMockHttpServletResponseAssert<MvcResultAssert, AssertableMvcResult> {

	MvcResultAssert(AssertableMvcResult mvcResult, @Nullable GenericHttpMessageConverter<Object> jsonMessageConverter) {
		super(jsonMessageConverter, mvcResult, MvcResultAssert.class);
	}

	@Override
	protected MockHttpServletResponse getResponse() {
		checkHasNotFailedUnexpectedly();
		return this.actual.getResponse();
	}

	/**
	 * Verify that the request has failed with an unresolved exception, and
	 * return a new {@linkplain AbstractThrowableAssert assertion} object
	 * that uses the unresolved {@link Exception} as the object to test.
	 */
	public AbstractThrowableAssert<?, ? extends Throwable> unresolvedException() {
		hasUnresolvedException();
		return Assertions.assertThat(this.actual.getUnresolvedException());
	}

	/**
	 * Return a new {@linkplain AbstractMockHttpServletRequestAssert assertion}
	 * object that uses the {@link MockHttpServletRequest} as the object to test.
	 */
	public AbstractMockHttpServletRequestAssert<?> request() {
		checkHasNotFailedUnexpectedly();
		return new MockHttpRequestAssert(this.actual.getRequest());
	}

	/**
	 * Return a new {@linkplain CookieMapAssert assertion} object that uses the
	 * response's {@linkplain Cookie cookies} as the object to test.
	 */
	public CookieMapAssert cookies() {
		checkHasNotFailedUnexpectedly();
		return new CookieMapAssert(this.actual.getResponse().getCookies());
	}

	/**
	 * Return a new {@linkplain MediaTypeAssert assertion} object that uses the
	 * response's {@linkplain MediaType content type} as the object to test.
	 */
	public MediaTypeAssert contentType() {
		checkHasNotFailedUnexpectedly();
		return new MediaTypeAssert(this.actual.getResponse().getContentType());
	}

	/**
	 * Return a new {@linkplain HandlerResultAssert assertion} object that uses
	 * the handler as the object to test. For a method invocation on a
	 * controller, this is relative method handler
	 * <p>Example: <pre><code class='java'>
	 * // Check that a GET to "/greet" is invoked on a "handleGreet" method name
	 * assertThat(mvc.perform(get("/greet")).handler().method().hasName("sayGreet");
	 * </code></pre>
	 */
	public HandlerResultAssert handler() {
		checkHasNotFailedUnexpectedly();
		return new HandlerResultAssert(this.actual.getHandler());
	}

	/**
	 * Verify that a {@link ModelAndView} is available and return a new
	 * {@linkplain ModelAssert assertion} object that uses the
	 * {@linkplain ModelAndView#getModel() model} as the object to test.
	 */
	public ModelAssert model() {
		checkHasNotFailedUnexpectedly();
		return new ModelAssert(getModelAndView().getModel());
	}

	/**
	 * Verify that a {@link ModelAndView} is available and return a new
	 * {@linkplain AbstractStringAssert assertion} object that uses the
	 * {@linkplain ModelAndView#getViewName()} view name} as the object to test.
	 * @see #hasViewName(String)
	 */
	public AbstractStringAssert<?> viewName() {
		checkHasNotFailedUnexpectedly();
		return Assertions.assertThat(getModelAndView().getViewName()).as("View name");
	}

	/**
	 * Return a new {@linkplain MapAssert assertion} object that uses the
	 * "output" flash attributes saved during request processing as the object
	 * to test.
	 */
	public MapAssert<String, Object> flash() {
		checkHasNotFailedUnexpectedly();
		return new MapAssert<>(this.actual.getFlashMap());
	}

	/**
	 * Verify that an {@linkplain AbstractHttpServletRequestAssert#hasAsyncStarted(boolean)
	 * asynchronous processing has started} and return a new
	 * {@linkplain ObjectAssert assertion} object that uses the asynchronous
	 * result as the object to test.
	 */
	public ObjectAssert<Object> asyncResult() {
		request().hasAsyncStarted(true);
		return Assertions.assertThat(this.actual.getAsyncResult()).as("Async result");
	}

	/**
	 * Verify that the request has failed with an unresolved exception.
	 * @see #unresolvedException()
	 */
	public MvcResultAssert hasUnresolvedException() {
		Assertions.assertThat(this.actual.getUnresolvedException())
				.withFailMessage("Expecting request to have failed but it has succeeded").isNotNull();
		return this;
	}

	/**
	 * Verify that the request has not failed with an unresolved exception.
	 */
	public MvcResultAssert doesNotHaveUnresolvedException() {
		Assertions.assertThat(this.actual.getUnresolvedException())
				.withFailMessage("Expecting request to have succeeded but it has failed").isNull();
		return this;
	}

	/**
	 * Verify that the actual mvc result matches the given {@link ResultMatcher}.
	 * @param resultMatcher the result matcher to invoke
	 */
	public MvcResultAssert matches(ResultMatcher resultMatcher) {
		checkHasNotFailedUnexpectedly();
		return super.satisfies(resultMatcher::match);
	}

	/**
	 * Apply the given {@link ResultHandler} to the actual mvc result.
	 * @param resultHandler the result matcher to invoke
	 */
	public MvcResultAssert apply(ResultHandler resultHandler) {
		checkHasNotFailedUnexpectedly();
		return satisfies(resultHandler::handle);
	}

	/**
	 * Verify that a {@link ModelAndView} is available with a view equals to
	 * the given one. For more advanced assertions, consider using
	 * {@link #viewName()}
	 * @param viewName the expected view name
	 */
	public MvcResultAssert hasViewName(String viewName) {
		viewName().isEqualTo(viewName);
		return this.myself;
	}


	@SuppressWarnings("NullAway")
	private ModelAndView getModelAndView() {
		ModelAndView modelAndView = this.actual.getModelAndView();
		Assertions.assertThat(modelAndView).as("ModelAndView").isNotNull();
		return modelAndView;
	}

	protected void checkHasNotFailedUnexpectedly() {
		Exception unresolvedException = this.actual.getUnresolvedException();
		if (unresolvedException != null) {
			throw Failures.instance().failure(this.info,
					new RequestFailedUnexpectedly(unresolvedException));
		}
	}

	private static final class MockHttpRequestAssert extends AbstractMockHttpServletRequestAssert<MockHttpRequestAssert> {

		private MockHttpRequestAssert(MockHttpServletRequest request) {
			super(request, MockHttpRequestAssert.class);
		}
	}

	private static final class RequestFailedUnexpectedly extends BasicErrorMessageFactory {

		private RequestFailedUnexpectedly(Exception ex) {
			super("%nRequest has failed unexpectedly:%n%s", unquotedString(getIndentedStackTraceAsString(ex)));
		}

		private static String getIndentedStackTraceAsString(Throwable ex) {
			String stackTrace = getStackTraceAsString(ex);
			return indent(stackTrace);
		}

		private static String getStackTraceAsString(Throwable ex) {
			StringWriter writer = new StringWriter();
			PrintWriter printer = new PrintWriter(writer);
			ex.printStackTrace(printer);
			return writer.toString();
		}

		private static String indent(String input) {
			BufferedReader reader = new BufferedReader(new StringReader(input));
			StringWriter writer = new StringWriter();
			PrintWriter printer = new PrintWriter(writer);
			reader.lines().forEach(line -> {
				printer.print(" ");
				printer.println(line);
			});
			return writer.toString();
		}

	}

}
