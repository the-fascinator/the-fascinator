<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" exclude-result-prefixes="oai_dc dc">

  <!-- create a Solr document for each dublin core record -->
  <xsl:template match="oai_dc:dc">
    <add allowDups="false">
      <doc>
        <xsl:apply-templates />
      </doc>
    </add>
  </xsl:template>

  <!-- add a Solr field for every dublin core field-->
  <xsl:template match="dc:*">
    <field name="dc_{local-name()}">
      <xsl:apply-templates />
    </field>
  </xsl:template>

  <!-- normalise whitespace for text -->
  <xsl:template match="text()">
    <xsl:value-of select="normalize-space(.)" />
  </xsl:template>

</xsl:stylesheet>
