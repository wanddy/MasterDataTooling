package org.bonitasoft.tools;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pablo Alonso de Linaje García
 */
public class MasterDataMergeTest {
    @Test
    public void testRegex(){
        String content = "<businessObjects>\n" +
                "        <businessObject qualifiedName=\"com.company.custom.MasterData\">\n" +
                "            <fields>\n" +
                "                <field type=\"STRING\" length=\"255\" name=\"name\" nullable=\"true\" collection=\"false\"/>\n" +
                "                <field type=\"STRING\" length=\"255\" name=\"valueDefault\" nullable=\"true\" collection=\"false\"/>\n" +
                "                <field type=\"STRING\" length=\"255\" name=\"valueAlternative\" nullable=\"true\" collection=\"false\"/>\n" +
                "                <field type=\"STRING\" length=\"255\" name=\"application\" nullable=\"true\" collection=\"false\"/>\n" +
                "                <field type=\"STRING\" length=\"255\" name=\"masterData\" nullable=\"true\" collection=\"false\"/>\n" +
                "                <field type=\"INTEGER\" length=\"255\" name=\"orderPosition\" nullable=\"true\" collection=\"false\"/>\n" +
                "                <field type=\"BOOLEAN\" length=\"255\" name=\"enabled\" nullable=\"true\" collection=\"false\"/>\n" +
                "            </fields>\n" +
                "            <uniqueConstraints/>\n" +
                "            <queries>\n" +
                "                <query name=\"selectApplications\" content=\"SELECT m &#xA;FROM MasterData m &#xA;WHERE &#xA;m.application = 'Framework'&#xA;AND m.masterData = 'Applications'&#xD;&#xA;AND m.enabled = true&#xA;ORDER BY m.orderPosition ASC\" returnType=\"java.util.List\">\n" +
                "                    <queryParameters/>\n" +
                "                </query>";


        Assert.assertEquals("com.company.custom", MasterDataMerge.findPackageName(content));


    }

}