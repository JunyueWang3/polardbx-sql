// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: DumperServer.proto

package com.alibaba.polardbx.rpc.cdc;

public interface DumpStreamOrBuilder extends
    // @@protoc_insertion_point(interface_extends:dumper.DumpStream)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>bytes payload = 1;</code>
   * @return The payload.
   */
  com.google.protobuf.ByteString getPayload();

  /**
   * <code>bool isHeartBeat = 2;</code>
   * @return The isHeartBeat.
   */
  boolean getIsHeartBeat();
}
