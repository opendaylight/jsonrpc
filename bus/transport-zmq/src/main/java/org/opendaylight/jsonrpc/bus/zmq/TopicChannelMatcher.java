/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelMatcher;

/**
 * {@link ChannelMatcher} to match PUB/SUB topic.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
public class TopicChannelMatcher implements ChannelMatcher {
    private final String topic;

    public TopicChannelMatcher(final String topic) {
        this.topic = topic;
    }

    @Override
    public boolean matches(Channel channel) {
        final String channelTopic = channel.attr(Constants.ATTR_PUBSUB_TOPIC).get();
        if (channelTopic == null) {
            // subscriber didn't sent subscription request with topic
            return true;
        } else {
            return topic.startsWith(channelTopic);
        }
    }
}
