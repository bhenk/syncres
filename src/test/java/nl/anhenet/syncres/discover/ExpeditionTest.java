package nl.anhenet.syncres.discover;


import nl.anhenet.syncres.xml.Capability;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

public class ExpeditionTest extends AbstractRemoteTest {

  @Test
  public void createWellknownUri() throws Exception {
    String[][] expectations = {
      { "http://foo.com/.well-known/resourcesync", "http://foo.com/.well-known/resourcesync" },
      { "http://foo.com/", "http://foo.com/.well-known/resourcesync" },
      { "http://foo.com", "http://foo.com/.well-known/resourcesync" },

      { "http://foo.com/bar/.well-known/resourcesync", "http://foo.com/bar/.well-known/resourcesync" },
      { "http://foo.com/bar/", "http://foo.com/.well-known/resourcesync" },
      { "http://foo.com/bar", "http://foo.com/.well-known/resourcesync" },
      { "http://foo.com/bar?this=bla&that=so", "http://foo.com/.well-known/resourcesync" }
    };

    for (String[] expect : expectations) {
      URI uri = URI.create(expect[0]);
      URI wk = Expedition.createWellKnownUri(uri);
      //System.out.println(expect[0] + " -> " + expect[1] + " -> " + wk.toString());
      assertThat(wk, equalTo(URI.create(expect[1])));
    }
  }

  @Test
  public void exploreAndFindResults() throws Exception {
    String path = "/timbucto";
    String url = composePath(path);

    setUpServer(path);
    Expedition expedition = new Expedition(getHttpclient(), getRsContext());

    List<ResultIndex> indexes = expedition.explore(url);
    /*indexes.forEach(resultIndex -> resultIndex.getResultMap()
      .forEach((uri, result) -> System.out.println(uri + " "
        + result.getStatusCode() + " "
        + result.getOrdinal())));*/
    assertThat(indexes.size(), equalTo(4));

    int resultCount1 = indexes.stream()
      .map(ResultIndex::getResultMap)
      .mapToInt(Map::size)
      .sum();
    assertThat(resultCount1, equalTo(15));

    int deadEnds1 = indexes.stream()
      .map(ResultIndexPivot::new)
      .map(ResultIndexPivot::listErrorResults)
      .mapToInt(List::size)
      .sum();
    assertThat(deadEnds1, equalTo(12));

    // again with merged indexes
    // uri's http://localhost:xxxxx/timbucto will be merged
    setUpServer(path);
    ResultIndex index = expedition.exploreAndMerge(url);
    ResultIndexPivot pivot = new ResultIndexPivot((index));

    int resultCount = index.getResultMap().size();
    assertThat(resultCount, equalTo(14)); // 15 > 14, merged http://localhost:xxxxxx/timbucto

    int deadEnds = pivot.listErrorResults().size();
    assertThat(deadEnds, equalTo(11)); // 12 > 11, merged http://localhost:xxxxxx/timbucto

    int finds = pivot.listUrlsetResults().size();
    assertThat(finds, equalTo(3));

    int clCount = pivot.listUrlsetResults(Capability.CAPABILITYLIST).size();
    assertThat(clCount, equalTo(2));

    int descCount = pivot.listUrlsetResults(Capability.DESCRIPTION).size();
    assertThat(descCount, equalTo(1));
  }

  @Test
  public void listUrlLocations() throws Exception {
    String path = "/clariah/duck/soup";
    setUpServer(path);

    String url = composePath(path);
    Expedition expedition = new Expedition(getHttpclient(), getRsContext());
    ResultIndex index = expedition.exploreAndMerge(url);
    ResultIndexPivot pivot = new ResultIndexPivot((index));
    List<String> locs = pivot.listUrlLocations(Capability.CAPABILITYLIST);

    String[] expectedLocs = new String[] {
      composePath("/clariah/duck/soup/resourcelist.xml"),
      composePath("/clariah/duck/soup/resourcedump.xml"),
      composePath("/clariah/duck/soup/changelist.xml"),
      composePath("/clariah/duck/soup/changedump.xml"),
      composePath("/foo/resourcelist.xml"),
      composePath("/foo/resourcedump.xml"),
      composePath("/foo/changelist.xml"),
      composePath("/foo/changedump.xml")
    };
    assertThat(locs, containsInAnyOrder(expectedLocs));
  }

  private void setUpServer(String path) {
    getMockServer().reset();

    //description
    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath("/.well-known/resourcesync"),
        Times.unlimited())

      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withBody(createDescription(path))
      );

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath("/robots.txt"),
        Times.unlimited())

      .respond(HttpResponse.response()
        .withStatusCode(404)
        .withBody("No robots here.")
      );

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path),
        Times.unlimited()) // 2 x

      .respond(HttpResponse.response()
        .withStatusCode(404)
        .withBody("No foo.")
      );

    // capabilitylists
    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path + "/capabilitylist1.xml"),
        Times.unlimited())

      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withBody(createCapabilityList(path))
      );

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path + "/capabilitylist2.xml"),
        Times.unlimited())

      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withBody(createCapabilityList("/foo"))
      );
  }

  private String createDescription(String basePath) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" +
      "        xmlns:rs=\"http://www.openarchives.org/rs/terms/\">\n" +
      "  <rs:ln rel=\"describedby\"\n" +
      "         href=\"http://example.com/info_about_source.xml\"/>\n" +
      "  <rs:md capability=\"description\"/>\n" +
      "  <url>\n" +
      "      <loc>" + composePath(basePath + "/capabilitylist1.xml") + "</loc>\n" +
      "      <rs:md capability=\"capabilitylist\"/>\n" +
      "      <rs:ln rel=\"describedby\"\n" +
      "             href=\"http://example.com/info_about_set1_of_resources.xml\"/>\n" +
      "  </url>\n" +
      "  <url>\n" +
      "      <loc>" + composePath(basePath + "/capabilitylist2.xml") + "</loc>\n" +
      "      <rs:md capability=\"capabilitylist\"/>\n" +
      "      <rs:ln rel=\"describedby\"\n" +
      "             href=\"http://example.com/info_about_set2_of_resources.xml\"/>\n" +
      "  </url>\n" +
      "  <url>\n" +
      "      <loc>" + composePath(basePath + "/capabilitylist3.xml") + "</loc>\n" +
      "      <rs:md capability=\"capabilitylist\"/>\n" +
      "      <rs:ln rel=\"describedby\"\n" +
      "             href=\"http://example.com/info_about_set3_of_resources.xml\"/>\n" +
      "  </url>\n" +
      "</urlset>";
  }

  private String createCapabilityList(String basePath) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" +
      "        xmlns:rs=\"http://www.openarchives.org/rs/terms/\">\n" +
      "  <rs:ln rel=\"describedby\"\n" +
      "         href=\"http://example.com/info_about_set1_of_resources.xml\"/>\n" +
      "  <rs:ln rel=\"up\"\n" +
      "         href=\"" + composePath("/.well-known/resourcesync") + "\"/>\n" +
      "  <rs:md capability=\"capabilitylist\"/>\n" +
      "  <url>\n" +
      "      <loc>" + composePath(basePath + "/resourcelist.xml") + "</loc>\n" +
      "      <rs:md capability=\"resourcelist\"/>\n" +
      "  </url>\n" +
      "  <url>\n" +
      "      <loc>" + composePath(basePath + "/resourcedump.xml") + "</loc>\n" +
      "      <rs:md capability=\"resourcedump\"/>\n" +
      "  </url>\n" +
      "  <url>\n" +
      "      <loc>" + composePath(basePath + "/changelist.xml") + "</loc>\n" +
      "      <rs:md capability=\"changelist\"/>\n" +
      "  </url>\n" +
      "  <url>\n" +
      "      <loc>" + composePath(basePath + "/changedump.xml") + "</loc>\n" +
      "      <rs:md capability=\"changedump\"/>\n" +
      "  </url>\n" +
      "</urlset>";
  }
}
