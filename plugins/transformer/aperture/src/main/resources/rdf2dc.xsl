<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:dcterms="http://purl.org/dc/terms/"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:j.0="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
	xmlns:j.2="http://www.semanticdesktop.org/ontologies/2007/03/22/nco#"
>
<xsl:param name="filePath"/>

<!-- Only for file for now -->
<xsl:template match="rdf:Description">
	<xsl:copy><xsl:apply-templates select="@*|node()"/> 
	<xsl:if test="not(starts-with(@rdf:about, 'urn:'))">
		<dcterms:identifier><xsl:value-of select="@rdf:about"/></dcterms:identifier>
	</xsl:if>

	<xsl:if test="not(//dc:title/text())">
		<dcterms:title><xsl:value-of select="$filePath"/></dcterms:title>
	</xsl:if>
	</xsl:copy>
</xsl:template>

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="j.0:mimeType">
	<xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
	<dcterms:format><xsl:value-of select="."/></dcterms:format>
</xsl:template>

<xsl:template match="dc:title">
	<xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
	<dcterms:title><xsl:value-of select="."/></dcterms:title>
</xsl:template>

<xsl:template match="j.2:creator">
	<xsl:param name="resource" select="./@rdf:resource"/>
	<xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
	<!--Stripping the space -->
	<dcterms:creator><xsl:value-of select="translate(//rdf:Description[@rdf:about=$resource],' &#10;&#13;&#9;','')"/></dcterms:creator>
</xsl:template>

<xsl:template match="dc:date">
	<xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
	<dcterms:date><xsl:value-of select="."/></dcterms:date>
</xsl:template>


</xsl:stylesheet>