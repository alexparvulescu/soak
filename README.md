SOAK - a Scala library for Oak
==============================

![0.0.7](https://img.shields.io/badge/soak-0.0.7-blue.svg) *Latest release: 0.0.7 / July 20, 2016*

Build with Scala ```2.11.x``` and [Oak](https://jackrabbit.apache.org/oak/) ```1.5.x```

[![Oak 1.5.5](https://img.shields.io/badge/Oak-1.5.5-green.svg)](https://jackrabbit.apache.org/oak/)
[![Build Status](https://travis-ci.org/stillalex/soak.svg?branch=master)](https://travis-ci.org/stillalex/soak)
[![Coverage Status](https://coveralls.io/repos/stillalex/soak/badge.svg?branch=master&service=github)](https://coveralls.io/github/stillalex/soak?branch=master)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)


Features
--------
* ```Session``` Operations made simple
* ```Tree``` helpers
* ```PropertyState``` helpers
* ```Query``` helpers (WIP)
* ```OSGi``` support, even comes with an [OSGi initializer for the repository](src/main/scala/com/pfalabs/soak/osgi/OakService.scala#L28)

Sessions Examples
-----------------

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

OSGi Examples
-------------

```scala
import com.pfalabs.soak.osgi.OakService

@Component(immediate = true)
class CustomOakService extends OakService {

  @Activate
  def activate(context: ComponentContext) = doActivate(context)

  @Deactivate
  def deactivate() = doDeactivate()
}
```

Trees Examples
--------------

Select all child nodes of ```/test```, filter and transform them to ```Item``` instances:

```scala

    case class Item(name: String, choice: Option[String])

    def getFilteredAsItem(root: Root): Iterable[Item] = (root > "/test") /:/ (filterByChoice, treeToItem)

    def filterByChoice(t: Tree): Boolean = asS(t | "choice").getOrElse("").equals("yes")

    def treeToItem(t: Tree): Item = Item(t.name, asS(t | "choice"))

    val example = runAsAdmin(getFilteredAsItem)
```

Query Examples
--------------

Select all child nodes of ```/testQ``` that have ```@lang = 'en'``` and transform them to ```Item``` instances:

```scala

    def treeToItem(t: Tree): Item = Item(t.name, asI(t | "id").get, asS(t | "lang"))

    val xpathEn = "/jcr:root/testQ/element(*, oak:Unstructured)[@lang = 'en']"

    val example = val o1 = runAsAdmin(xpath(xpathEn, treeToItem))
```

Use it
------

```xml
<dependency>
  <groupId>com.pfalabs</groupId>
  <artifactId>com.pfalabs.soak_2.11</artifactId>
  <version>0.0.7</version>
</dependency>
```

The releases are published on [bintray](https://bintray.com/pfalabs/maven/soak), so make sure you add the repository to the ```pom.xml``` file

```xml
<repository>
  <id>bintray-pfalabs-maven</id>
  <name>bintray</name>
  <layout>default</layout>
  <url>http://dl.bintray.com/pfalabs/maven</url>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
</repository>
```

License
-------

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
