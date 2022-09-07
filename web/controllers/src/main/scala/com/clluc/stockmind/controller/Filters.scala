package com.clluc.stockmind.controller

import javax.inject.Inject

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSFilter
import play.filters.headers.SecurityHeadersFilter

/**
  * Provides filters.
  */
class Filters @Inject()(
    securityHeadersFilter: SecurityHeadersFilter,
    corsFilter: CORSFilter
) extends HttpFilters {
  // Add filters as needed (i.e. CORS is needed to test the frontend on a web browser)
  override def filters: Seq[EssentialFilter] = Seq(corsFilter)
}
