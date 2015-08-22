SOAK - a Scala library for Oak
==============================

Build with Scala ```2.11.x``` and [Oak](https://jackrabbit.apache.org/oak/) ```1.3.x```

Features
--------
* Session Operations made simple
* OSGi initializer

Examples
--------

```scala
  "Session ops" should "create content" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    val o1 = runAsAdmin({ root =>
      root.getTree("/").addChild("test")
      root.commit()
    })
    assert(o1.isSuccess)

    val o2 = runAsAdmin({ root =>
      val t = root.getTree("/").getChild("test")
      assert(t.exists())
    })
    assert(o2.isSuccess)

    val o3 = Sessions.run("none", "")({ root =>
      fail("not allowed here!")
    })
    assert(o3.isFailure)
  }
```


Use it
------

```xml
<dependency>
  <groupId>com.pfalabs</groupId>
  <artifactId>com.pfalabs.soak_2.11</artifactId>
  <version>0.0.3</version>
</dependency>
```

The releases are published on [bintray](https://bintray.com/alexparvulescu/pfalabs/soak/view), so make sure you add the repository to the ```pom.xml``` file

```xml
<repository>
  <id>bintray-alexparvulescu-pfalabs</id>
  <name>bintray</name>
  <layout>default</layout>
  <url>http://dl.bintray.com/alexparvulescu/pfalabs</url>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
</repository>
```

License
-------

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
