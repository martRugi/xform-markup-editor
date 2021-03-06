package org.openxdata.markup.serializer

import org.custommonkey.xmlunit.DetailedDiff
import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.XMLTestCase
import org.openxdata.markup.Fixtures
import org.openxdata.markup.Util
import org.openxdata.markup.deserializer.MarkupDeserializer

/**
 * Created with IntelliJ IDEA.
 * User: kay
 * Date: 5/4/13
 * Time: 11:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class LayoutSerializerTest extends XMLTestCase {

    public void testGenerateLayout() throws Exception {

        LayoutSerializer ser = new LayoutSerializer()


        def form = new MarkupDeserializer(Fixtures.oxdSampleForm).study().forms[0]

        def xml = ser.generateLayout(form)

        // assertEquals Fixtures.xmlOxdSampleForm,xml

        DetailedDiff myDiff = new DetailedDiff(new Diff(Fixtures.xmlOxdSampleForm, xml));
        List allDifferences = myDiff.getAllDifferences();
        assertEquals(myDiff.toString(), 0, allDifferences.size())

//        assertXMLEqual Fixtures.xmlOxdSampleForm,xml

//        XFormSerializer serializer = new XFormSerializer()
//        println serializer.toXForm(form)
    }
}
