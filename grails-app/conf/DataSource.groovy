// environment specific settings
environments {
  development {
    dataSource {
      dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
      url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
      pooled = true
      driverClassName = "org.h2.Driver"
      username = "sa"
      password = ""
    }
    hibernate {
      cache.use_second_level_cache = true
      cache.use_query_cache = false
      cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
    }
  }
  test {
    dataSource {
      dbCreate = "update"
      url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000"

      pooled = true
      driverClassName = "org.h2.Driver"
      username = "sa"
      password = ""
    }
    hibernate {
      cache.use_second_level_cache = true
      cache.use_query_cache = false
      // cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
      cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    }
  }
}
