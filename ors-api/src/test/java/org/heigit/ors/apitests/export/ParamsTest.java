package org.heigit.ors.apitests.export;

import org.heigit.ors.apitests.common.EndPointAnnotation;
import org.heigit.ors.apitests.common.ServiceTest;
import org.heigit.ors.apitests.common.VersionAnnotation;
import org.heigit.ors.export.ExportErrorCodes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.heigit.ors.apitests.utils.CommonHeaders.jsonContent;

import org.hamcrest.Matchers;

@EndPointAnnotation(name = "export")
@VersionAnnotation(version = "v2")
class ParamsTest extends ServiceTest {
    public ParamsTest() {
        JSONArray coord1 = new JSONArray();
        coord1.put(0.001);
        coord1.put(0.001);
        JSONArray coord2 = new JSONArray();
        coord2.put(8.681495);
        coord2.put(49.41461);
        JSONArray coord3 = new JSONArray();
        coord3.put(8.686507);
        coord3.put(49.41943);

        JSONArray bboxMock = new JSONArray();
        bboxMock.put(coord1);
        bboxMock.put(coord1);

        JSONArray bboxFaulty = new JSONArray();
        bboxFaulty.put(coord1);

        JSONArray bboxProper = new JSONArray();
        bboxProper.put(coord2)
                .put(coord3);

        addParameter("bboxFake", bboxMock);
        addParameter("bboxFaulty", bboxFaulty);
        addParameter("bboxProper", bboxProper);
    }

    @Test
    void basicPingTest() {
        JSONObject body = new JSONObject();
        body.put("bbox", getParameter("bboxFake"));
        given()
                .headers(jsonContent)
                .pathParam("profile", "driving-car")
                .body(body.toString())
                .when()
                .post(getEndPointPath() + "/{profile}/topojson")
                .then()
                .statusCode(200);
    }

    @Test
    void expectUnknownProfile() {
        JSONObject body = new JSONObject();
        body.put("bbox", getParameter("bboxFake"));
        given()
                .headers(jsonContent)
                .pathParam("profile", "driving-car-123")
                .body(body.toString())
                .when()
                .post(getEndPointPath() + "/{profile}/json")
                .then()
                .assertThat()
                .body("error.code", Matchers.is(ExportErrorCodes.INVALID_PARAMETER_VALUE))
                .statusCode(400);
    }

    @Test
    void expectInvalidResponseFormat() {
        JSONObject body = new JSONObject();
        body.put("bbox", getParameter("bboxFake"));
        given()
                .headers(jsonContent)
                .pathParam("profile", "driving-car")
                .body(body.toString())
                .when()
                .post(getEndPointPath() + "/{profile}/blah")
                .then()
                .assertThat()
                .body("error.code", Matchers.is(ExportErrorCodes.UNSUPPORTED_EXPORT_FORMAT))
                .statusCode(406);
    }

    @Test
    void expectMissingBbox() {
        JSONObject body = new JSONObject();

        given()
                .headers(jsonContent)
                .pathParam("profile", "driving-car")
                .body(body.toString())
                .when()
                .post(getEndPointPath() + "/{profile}/json")
                .then()
                .assertThat()
                //not sure mismatched input is correct here, missing parameter seems more intuitive?
                .body("error.code", Matchers.is(ExportErrorCodes.MISMATCHED_INPUT))
                .statusCode(400);
    }

    @Test
    void expectFaultyBbox() {
        JSONObject body = new JSONObject();
        body.put("bbox", getParameter("bboxFaulty"));
        given()
                .headers(jsonContent)
                .pathParam("profile", "driving-car")
                .body(body.toString())
                .when()
                .post(getEndPointPath() + "/{profile}/topojson")
                .then()
                .assertThat()
                .body("error.code", Matchers.is(ExportErrorCodes.INVALID_PARAMETER_VALUE))
                .statusCode(400);
    }

    @Test
    void expectNodesAndEdges() {
        JSONObject body = new JSONObject();
        body.put("bbox", getParameter("bboxProper"));
        given()
                .headers(jsonContent)
                .pathParam("profile", "driving-car")
                .body(body.toString())
                .when()
                .post(getEndPointPath() + "/{profile}/json")
                .then()
                .assertThat()
                .body("containsKey('nodes')", is(true))
                .body("containsKey('edges')", is(true))
                .body("containsKey('nodes_count')", is(true))
                .body("containsKey('edges_count')", is(true))
                .statusCode(200);
    }

    @Test
    void expectTopoJsonFormat() {
        JSONObject body = new JSONObject();
        body.put("bbox", getParameter("bboxProper"));
        given()
                .headers(jsonContent)
                .pathParam("profile", "wheelchair")
                .body(body.toString())
                .when()
                .post(getEndPointPath() + "/{profile}/topojson")
                // TODO: remove me before merging to main
                .then().log().all()
                .assertThat()
                .body("type", is("Topology"))
                .body("objects.size()", is(1))
                .body("objects.network.type", is("GeometryCollection"))
                .body("bbox.size()", is(4))
                // approximations due to float comparison
                .body("bbox[0]", is(8.681522F))
                .body("bbox[1]", is(49.41491F))
                .body("bbox[2]", is(8.686507F))
                .body("bbox[3]", is(49.41943F))
                .body("arcs.size()", is(128))
                .body("objects.network.geometries.size()", is(128))
                .statusCode(200);
    }
}
