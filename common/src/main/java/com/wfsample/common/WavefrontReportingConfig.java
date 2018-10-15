package com.wfsample.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;

/**
 * Configuration for reporting telemetry to wavefront either through direct ingestion or through
 * wavefront proxy.
 *
 * @author Srujan Narkedamalli (snarkedamall@wavefront.com).
 */
public class WavefrontReportingConfig {

  /**
   * Reporting mechanism to be used.
   */
  @JsonProperty
  @Nonnull
  private Mechanism reportingMechanism;

  /**
   * Wavefront server to be used for direct ingestion.
   */
  @JsonProperty
  private String server;

  /**
   *  Wavefront API token for direct ingestion.
   */
  @JsonProperty
  private String token;

  /**
   * Proxy host for proxy ingestion.
   */
  @JsonProperty
  private String proxyHost;

  /**
   * Proxy port for metrics ingestion.
   */
  @JsonProperty
  private int proxyMetricsPort;

  /**
   * Proxy port for distributions ingestion.
   */
  @JsonProperty
  private int proxyDistributionsPort;

  /**
   * Proxy port for trace/span ingestion.
   */
  @JsonProperty
  private int proxyTracingPort;

  /**
   * Interval at which to report telemetry data.
   */
  @JsonProperty
  private int flushIntervalSeconds = 1;

  /**
   * Host/Source to be used for the telemetry data.
   */
  @JsonProperty
  private String dataSource;

  /**
   * Wavefront reporting mechanism.
   */
  public enum Mechanism {
    DIRECT,
    PROXY;
  }

  @Nonnull
  public Mechanism getReportingMechanism() {
    return reportingMechanism;
  }

  public String getServer() {
    return server;
  }

  public String getToken() {
    return token;
  }

  public String getProxyHost() {
    return proxyHost;
  }

  public int getProxyMetricsPort() {
    return proxyMetricsPort;
  }

  public int getProxyDistributionsPort() {
    return proxyDistributionsPort;
  }

  public int getFlushIntervalSeconds() {
    return flushIntervalSeconds;
  }

  public String getDataSource() {
    return dataSource;
  }

  public int getProxyTracingPort() {
    return proxyTracingPort;
  }
}
