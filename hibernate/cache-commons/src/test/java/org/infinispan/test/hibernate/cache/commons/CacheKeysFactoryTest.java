package org.infinispan.test.hibernate.cache.commons;

import org.hibernate.SessionFactory;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.CacheImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.infinispan.test.hibernate.cache.commons.functional.entities.Name;
import org.infinispan.test.hibernate.cache.commons.functional.entities.Person;
import org.infinispan.test.hibernate.cache.commons.util.InfinispanTestingSetup;
import org.infinispan.test.hibernate.cache.commons.util.TestInfinispanRegionFactory;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.infinispan.Cache;
import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import static org.infinispan.test.hibernate.cache.commons.util.TxUtil.withTxSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CacheKeysFactoryTest extends BaseUnitTestCase {
   @Rule
   public InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

   private SessionFactory getSessionFactory(String cacheKeysFactory) {
      Configuration configuration = new Configuration()
         .setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true")
         .setProperty(Environment.CACHE_REGION_FACTORY, TestInfinispanRegionFactory.class.getName())
         .setProperty(Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "transactional")
         .setProperty(AvailableSettings.SHARED_CACHE_MODE, "ALL")
         .setProperty(Environment.HBM2DDL_AUTO, "create-drop");
      if (cacheKeysFactory != null) {
         configuration.setProperty(Environment.CACHE_KEYS_FACTORY, cacheKeysFactory);
      }
      configuration.addAnnotatedClass(Person.class);
      return configuration.buildSessionFactory();
   }

   @Test
   public void testNotSet() throws Exception {
      test(null, "CacheKeyImplementation");
   }

   @Test
   public void testDefault() throws Exception {
      test(DefaultCacheKeysFactory.SHORT_NAME, "CacheKeyImplementation");
   }

   @Test
   public void testDefaultClass() throws Exception {
      test(DefaultCacheKeysFactory.class.getName(), "CacheKeyImplementation");
   }

   @Test
   public void testSimple() throws Exception {
      test(SimpleCacheKeysFactory.SHORT_NAME, Name.class.getSimpleName());
   }

   @Test
   public void testSimpleClass() throws Exception {
      test(SimpleCacheKeysFactory.class.getName(), Name.class.getSimpleName());
   }

   private void test(String cacheKeysFactory, String keyClassName) throws Exception {
      SessionFactory sessionFactory = getSessionFactory(cacheKeysFactory);
      withTxSession(false, sessionFactory, s -> {
         Person person = new Person("John", "Black", 39);
         s.persist(person);
      });

      TestInfinispanRegionFactory regionFactory = (TestInfinispanRegionFactory) ((CacheImplementor) sessionFactory.getCache()).getRegionFactory();
      Cache<Object, Object> cache = regionFactory.getCacheManager().getCache(Person.class.getName());
      Iterator<Object> iterator = cache.getAdvancedCache().getDataContainer().keySet().iterator();
      assertTrue(iterator.hasNext());
      Object key = iterator.next();
      assertEquals(keyClassName, key.getClass().getSimpleName());

      withTxSession(false, sessionFactory, s -> {
         Person person = s.load(Person.class, new Name("John", "Black"));
         assertEquals(39, person.getAge());
      });
   }
}