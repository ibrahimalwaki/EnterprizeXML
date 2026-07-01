<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" encoding="UTF-8" indent="yes" doctype-system="about:legacy-compat"/>

    <!-- XPath 1.0 cannot sum a per-row product expression, so the total is computed
         in Java (it's already stored on the persisted order) and passed in here. -->
    <xsl:param name="orderTotal"/>

    <xsl:template match="/purchaseOrder">
        <html lang="en">
            <head>
                <meta charset="UTF-8"/>
                <title>Invoice <xsl:value-of select="orderId"/></title>
                <style>
                    body { font-family: Arial, Helvetica, sans-serif; margin: 2rem; color: #222; }
                    h1 { font-size: 1.4rem; }
                    table { border-collapse: collapse; width: 100%; margin-top: 1rem; }
                    th, td { border: 1px solid #ccc; padding: 0.5rem 0.75rem; text-align: left; }
                    th { background-color: #f2f2f2; }
                    .meta td { border: none; padding: 0.2rem 0.75rem 0.2rem 0; }
                    .total-row td { font-weight: bold; }
                    .text-right { text-align: right; }
                </style>
            </head>
            <body>
                <h1>Invoice for Order <xsl:value-of select="orderId"/></h1>

                <table class="meta">
                    <tr>
                        <td><strong>Customer ID:</strong></td>
                        <td><xsl:value-of select="customerId"/></td>
                    </tr>
                    <tr>
                        <td><strong>Order Date:</strong></td>
                        <td><xsl:value-of select="orderDate"/></td>
                    </tr>
                </table>

                <table>
                    <thead>
                        <tr>
                            <th>SKU</th>
                            <th>Description</th>
                            <th class="text-right">Quantity</th>
                            <th class="text-right">Unit Price</th>
                            <th class="text-right">Line Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:for-each select="items/item">
                            <tr>
                                <td><xsl:value-of select="sku"/></td>
                                <td><xsl:value-of select="description"/></td>
                                <td class="text-right"><xsl:value-of select="quantity"/></td>
                                <td class="text-right"><xsl:value-of select="unitPrice"/></td>
                                <td class="text-right">
                                    <xsl:value-of select="format-number(quantity * unitPrice, '0.00')"/>
                                </td>
                            </tr>
                        </xsl:for-each>
                        <tr class="total-row">
                            <td colspan="4" class="text-right">Total</td>
                            <td class="text-right">
                                <xsl:value-of select="$orderTotal"/>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>
