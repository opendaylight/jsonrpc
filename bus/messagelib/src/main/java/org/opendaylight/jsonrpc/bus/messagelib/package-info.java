/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * This package contains public API that can be used to create instances of
 * various session types and proxy objects.
 *
 * <p>
 * <strong>Requester proxy</strong> - send request to remote responder instance
 * and read response back. To create requester proxy, follow these steps:
 * <ol>
 * <li>create responder (on remote peer) by providing implementation of known
 * API</li>
 * <li>connect requester proxy to it (on local peer) using known API and
 * URL</li>
 * <li>send request and read response</li>
 * </ol>
 *
 * <pre>
 * interface SomeAPI {
 *     int method1(int num, String str);
 * }
 *
 * // Instance of this class is exposed on remote peer
 * class SomeService implements SomeAPI {
 *     &#64;Override
 *     int method1(int num, String str) {
 *         return 10;
 *     }
 * }
 * </pre>
 *
 * <p>
 * step 1 (remote peer)
 *
 * <pre>
 * TransportFactory tf = ...
 *
 * SomeService svc = tf.createResponder("zmq://192.168.1.10:20000", new SomeService());
 *
 * </pre>
 *
 * <p>
 * step 2 (local peer)
 *
 * <pre>
 *
 * TransportFactory tf = ...
 * SomeService svc = tf.createRequesterProxy(SomeAPI.class, "zmq://192.168.1.10:20000");
 *
 * </pre>
 *
 * <p>
 * step 3 (local peer)
 *
 * <pre>
 * int x = svc.method1(1, "ABC"); // return value assigned to x is 10 (see
 *                                // implementation above)
 * </pre>
 *
 * <p>
 * This will result in following communication on bus:
 *
 * <pre>
 * &gt; { "id" : 1, "jsonrpc" : "2.0", "method" : "method1", params: [1, "ABC"] }
 * &lt; { "id" : 1, "jsonrpc" : "2.0", "result" : 10 }
 * </pre>
 *
 * <p>
 * <strong>Publisher proxy</strong> - send notification to all channels
 * subscribed on remote publisher instance. To create publisher proxy instance,
 * follow these steps:
 * <ol>
 * <li>create publisher proxy (provide interface and URL - local peer)</li>
 * <li>connect (multiple) subscribers to it (remote peers)</li>
 * <li>invoke notification method on publisher proxy (provided interface)</li>
 * </ol>
 *
 * <p>
 * Example of API:
 *
 * <pre>
 * interface SomeAPI {
 *     void notification_method1(String info); // return type must be void
 * }
 *
 * class SomeSubscriber implements SomeAPI {
 *     &#64;Override
 *     void notification_method1(String info) {
 *         // this method will be called when notification comes in
 *     }
 * }
 * </pre>
 *
 * <p>
 * step 1 - local peer
 *
 * <pre>
 * TransportFactory tf = ...
 *
 * SomeAPI proxy = tf.createPublisherProxy(SomeAPI.class, "ws://192.168.1.12:12345");
 * </pre>
 *
 * <p>
 * step 2 - (multiple) remote peers
 *
 * <pre>
 * TransportFactory tf = ...
 * SomeSubscriber session = tf.createSubscriber("ws://192.168.1.12:12345", new SomeSubscriber());
 * </pre>
 *
 * <p>
 * step 3 - local peer
 *
 * <pre>
 * proxy.notification_method1("ABC")
 * </pre>
 *
 * <p>
 * This will send notification to all connected subscribers:
 * <pre>
 * { "jsonrpc" : "2.0", "method" : "notification_method1", params: ["ABC"] }
 * </pre>
 */
package org.opendaylight.jsonrpc.bus.messagelib;
