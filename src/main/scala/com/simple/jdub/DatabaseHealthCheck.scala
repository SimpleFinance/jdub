package com.simple.jdub

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result

class DatabaseHealthCheck(val database: Database, val name: String = "database") extends HealthCheck {
  def check() = if (database.ping()) {
    Result.healthy()
  } else {
    Result.unhealthy("1 wasn't equal to 1")
  }
}
