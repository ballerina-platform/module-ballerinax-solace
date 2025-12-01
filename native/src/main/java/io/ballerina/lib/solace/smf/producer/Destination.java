package io.ballerina.lib.solace.smf.producer;

/**
 * Sealed interface representing union of destination types.
 * Permits two destination types:
 * - Topic - publish/subscribe messaging
 * - Queue - point-to-point messaging
 */
public sealed interface Destination permits Topic, Queue {
}
