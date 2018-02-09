/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * <p>
 * HTTP transports session types support matrix
 * </p>
 * <table summary="Support matrix" border="1">
 * <tr>
 * <td>Transport</td>
 * <td>Requester</td>
 * <td>Responder</td>
 * <td>Publisher</td>
 * <td>Subscriber</td>
 * </tr>
 * <tr>
 * <td>HTTP</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * <td>No</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>HTTPs</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * <td>No</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>WS</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * </tr>
 * <tr>
 * <td>WSS</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * </tr>
 * </table>
 * <p>
 * Implementations details
 * </p>
 *
 */

package org.opendaylight.jsonrpc.bus.http;
