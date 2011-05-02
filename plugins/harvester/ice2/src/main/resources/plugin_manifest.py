
#    Copyright (C) 2008  Distance and e-Learning Centre,
#    University of Southern Queensland
#
#    This program is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program; if not, write to the Free Software
#    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#

""" """

import types
try:
    import Set
except:
    Set = set


pluginName = "ice.manifest"
pluginDesc = ""
pluginFunc = None           # either (or both) pluginFunc or pluginClass should
pluginClass = None          #   be set by the pluginInit() method
pluginInitialized = False   # set to True by pluginInit() method


def pluginInit(iceContext, **kwargs):
    global pluginFunc, pluginClass, pluginInitialized
    pluginFunc = None
    pluginClass = Manifest
    pluginInitialized = True
    return pluginFunc


class Manifest(object):
    # Notes:
    #   If the package is copied all of the itemGuids will change!
    #       This will handle that ok (via updateItems())
    # Constructor
    #   __init__(iceContext)
    # Properties
    #   title
    #   homePageItem
    #   children        (ReadOnly) # _ManifestItems
    # Methods
    #   getManifestItem(itemGuid)
    #   getManifestItemForRenditionName(packageRelativeRenditionName)
    #   updateItems(packageItem)
    #   moveItem(itemGuid, toParentItemGuid, toPosition=99999)
    #   getParentOf(manifestItem)
    #
    def __init__(self, iceContext):
        #self.iceContext = iceContext            # non-serializable  (if needed later)
        self.__manifestItem = _ManifestItem(None, "")
        self.__allManifestItems = {}            # itemGuid:ManifestItem(s)
        #self.__title = None                    # "[Untitled]"
        self.__homePageItem = None              # None == TOC

    #def __getstate__(self):
    #    d = dict(self.__dict__)
    #    d.pop("iceContext")
    #def __setstate__(self, d):
    #    self.iceContext = None

    @property
    def allManifestItems(self):
        if self.__allManifestItems.has_key(None):
            self.__allManifestItems.pop(None)
        mItems = self.__allManifestItems.values()
        #try:
        #    mItems.remove(self.__manifestItem)
        #except: pass
        return mItems

    def __getTitle(self):
        #return self.__title
        t = self.__manifestItem.manifestTitle
        return t
    def __setTitle(self, title):
        #self.__title = title
        self.__manifestItem.manifestTitle = title
    title = property(__getTitle, __setTitle)
    manifestTitle = property(__getTitle, __setTitle)


    def __getHomePageItem(self):
        return self.__homePageItem
    def __setHomePageItem(self, manifestItem):
        self.__homePageItem = manifestItem
    homePageItem = property(__getHomePageItem, __setHomePageItem)


    @property
    def children(self):
        return self.__manifestItem.children

    @property
    def _children(self):
        return self.__manifestItem._children

    @property
    def itemGuid(self):
        return "[root]"


    def getManifestItem(self, itemGuid=None):
        if itemGuid is None:
            return self.__manifestItem
        return self.__allManifestItems.get(itemGuid)


    def getManifestItemForRenditionName(self, renditionName):
        for i in self.__allManifestItems.values():
            if i.renditionName==renditionName:
                return i
        return None


    def updateItems(self, packageItem):
        #print "*** updateItems ***"
        packageRelPath = packageItem.relPath
        lenPackageRelPath = len(packageRelPath)
        items = self.__getAllPackageItems(packageItem)
        # Note: currently this will not get items that are outside of the package

        relPaths = {}
        for item in self.__allManifestItems.values():
            if item is None:
                continue
            relPaths[item.relPath] = item
        # Check for any copied or moved (and replaced) items and fix up
        # first check/update manifestItems based on relPaths
        for item in items:
            relPath = item.relPath[lenPackageRelPath:]
            mItem = relPaths.get(relPath)
            if mItem is None:
                continue
            if mItem.itemGuid!=item.guid:
                #try:
                #    print "*** relPath='%s', mItem=%s" % (relPath, mItem)
                #except Exception, e:
                #    print "### *** relPath='?' Error='%s'" % str(e)
                # the item has been copied to a new package
                #   or has been replaced by another item (with the same name)
                self.__allManifestItems.pop(mItem.itemGuid)
                if item.guid is not None:
                    mItem.itemGuid = item.guid
                    self.__allManifestItems[mItem.itemGuid] = mItem
        # then check/update manifestItems based on item.guids
        for item in items:
            mItem = self.getManifestItem(item.guid)
            if mItem is None:
                continue
            if mItem.relPath!=item.relPath[lenPackageRelPath:]:
                # the item has been moved within the package
                mItem.relPath = item.relPath[lenPackageRelPath:]

        itemGuids = [i.guid for i in items]
        # get all items that are in __allManifestItems that do not exist in items
        keys = self.__allManifestItems.keys()
        s = set(keys)
        deletedSet = s.difference(itemGuids)
        for deletedGuid in deletedSet:
            self.__deleteItem(deletedGuid)
        # add any new/added items
        count = 0
        for item in items:
            itemGuid = item.guid
            if itemGuid is None:
                continue
            if itemGuid not in self.__allManifestItems.keys():
                count += 1
                relPath = item.relPath[lenPackageRelPath:]
                self.__addManifestItem(itemGuid, relPath)
            else:
                #print "** '%s'" % item.relPath
                #ti = packageItem.iceContext.rep.getItem(item.relPath)
                #ti._createNewGuid()
                #print ti
                i = self.getManifestItem(itemGuid)
                #print "itemGuid = '%s'" % itemGuid
                #print "i.itemGuid='%s'" % i.itemGuid
                p = self.__manifestItem.getParentOf(i)
                if p is None:
                    pass
                    #print "i='%s'" % i
                    #print "child='%s'" % i.children[0]
                    #print "adding '%s' item.relPath='%s'" % (i.renditionName, item.relPath)
                    #self.__manifestItem._insertManifestItem(i)
            mItem = self.getManifestItem(itemGuid)
            if mItem is not None:
                mItem.docTitle = item.getMeta("title")
                rName = mItem.relPath
                if item.hasHtml:
                    rName = rName[:-len(item.ext)]+".htm"
                elif item.hasPdf:
                    rName = rName[:-len(item.ext)]+".pdf"
                mItem.renditionName = rName
        # An external referenced ice items
        #for item in items:
        for item in []:
            if item.hasHtml:
                data = item.getRendition(".xhtml.body")
                if data is None:                                #
                    #print " *** relPath='%s'" % item.relPath   #
                    continue                                    #
                #print item.relPath, len(data)
                HtmlLinks = item.iceContext.getPluginClass("ice.HtmlLinks")
                htmlLinks = HtmlLinks(item.iceContext, data)
                #print "--"
                for att in htmlLinks.getUrlAttributes():
                    #print att
                    if att.protocol.startswith("http") and att.path.startswith("/rep."):
                        # add external item
                        guid = packageItem.iceContext.md5Hex(att.url)
                        #print " *** %s" % guid
                        self.__addManifestItem(guid, att.url)
                    elif att.path.startswith("../"):
                        fs = item.iceContext.fs
                        rPath = fs.absPath(fs.join(item.relDirectoryPath, att.url))
                        if not rPath.startswith(packageItem.relDirectoryPath):
                            i = item.getIceItem(rPath)
                            if i is not None:
                                iGuid = i.guid
                                if iGuid not in self.__allManifestItems.keys():
                                    self.__addManifestItem(iGuid, i.relPath)
                            else:
                                print "Warning: referenced item '%s' does not exist" % rPath
                #print "--"
                #print htmlLinks
                htmlLinks.close()
            elif item.ext==".htm" or item.ext==".html":
                print item.relPath
        #print "**** %s, %s" % (len(items), count)


    def insertBefore(self, beforeItemGuid, itemGuid):
        beforeManifestItem = self.getManifestItem(beforeItemGuid)
        if beforeManifestItem is None:
            return
        # get the parent
        parent = self.__manifestItem.getParentOf(beforeManifestItem)
        # get the items index
        index = parent.children.index(beforeManifestItem)
        self.moveItem(itemGuid, parent.itemGuid, index)


    def addTo(self, addToItemGuid, itemGuid):
        self.moveItem(itemGuid, addToItemGuid)


    def moveItem(self, itemGuid, toParentItemGuid, toPosition=99999):
        toParentItem = self.getManifestItem(toParentItemGuid)
        item = self.getManifestItem(itemGuid)
        if item is None:
            return
        if toParentItem.hasChild(item):
            fromParentItem = toParentItem
            # OK just changing the position within it's parent list
            # get the current position of the item
            pos = toParentItem._children.index(item)
            if pos<toPosition:
                toPosition -= 1
            #print "toParentItem.hasChild pos='%s'" % toPosition
        else:
            fromParentItem = self.__manifestItem.getParentOf(item)
            if fromParentItem is None:
                print "*** cannot find fromParentItem!"
                return
        fromParentItem._removeManifestItem(item)
        toParentItem._insertManifestItem(item, toPosition)


    def getParentOf(self, manifestItem):
        p = self.__manifestItem.getParentOf(manifestItem)
        return p


    def asJSON(self, jsonWriter=None):
        if jsonWriter is None:
            #self.iceContext.jsonWrite()
            def jsonWriter(d):
                return d
        d = {}
        if self.__homePageItem:
            d["homePage"] = self.__homePageItem.renditionName
        else:
            d["homePage"] = "toc.htm"
        d["title"] = self.title
        def r(l, mItem):
            children = []
            for child in mItem.children:
                r(children, child)
            d = {"title":mItem.title, "relPath":mItem.renditionName,
                 "visible":not mItem.isHidden, "children":children}
            l.append(d)
            return l
        toc = r([], self.__manifestItem)[0]["children"]
        d["toc"] = toc
        return jsonWriter(d);


    def __deleteItem(self, itemGuid):
        manifestItem = self.getManifestItem(itemGuid)
        if manifestItem is not None:
            if self.__homePageItem==manifestItem:
                self.__homePageItem = None
            self.__allManifestItems.pop(itemGuid)
            # get the manifestItem's parent item
            parentManifestItem = self.__manifestItem.getParentOf(manifestItem)
            if parentManifestItem is not None:
                parentManifestItem._deleteManifestItem(manifestItem)
            else:
                print "*** parentManifestItem not found!"
        return manifestItem


    def __addManifestItem(self, itemGuid, relPath):
        manifestItem = self.getManifestItem(itemGuid)
        if manifestItem is None:
            manifestItem = _ManifestItem(itemGuid, relPath)
            self.__allManifestItems[itemGuid] = manifestItem
            self.__manifestItem._insertManifestItem(manifestItem)
        return manifestItem


    def __getAllPackageItems(self, packageItem):
        items = []
        for listItems in packageItem.walk():
            for item in list(listItems):
                if item.isFile:
                    if item.hasProperties:
                        items.append(item)
                if item.isDirectory:
                    #if not item.hasProperties or item.name=="media":
                    if not item.hasProperties:
                        listItems.remove(item)
        items = [i for i in items if not i.isMissing]
        return items


    def __str__(self):
        s = "[Manifest] title='%s' children=%s, items=%s" % (self.title,
                len(self.children), len(self.__allManifestItems)-1)
        return s



class _ManifestItem(object):
    # Constructor
    #   __init__(itemGuid, relPath)
    # Properties
    #   itemGuid
    #   relPath
    #   children
    #   _children       # raw children
    #   title           # None if no title has been set
    #   isHidden
    #   hasChildren
    #   docTitle
    #   renditionName
    # Methods
    #   hasChild(manifestItem)
    #   hasDesendent(manifestItem)
    #   getParentOf(manifestItem)
    #
    def __init__(self, itemGuid, relPath):
        self.__title = None
        self.__hidden = False
        self.__itemGuid = itemGuid
        self.__relPath = relPath
        self.__manifestItems = []       # List of ManifestItems(s) objects
        self.__docTitle = None
        self.__renditionName = ""
        if self.__relPath.find("media/")!=-1:
            self.__hidden = True


    def __getItemGuid(self):
        return self.__itemGuid
    def __setItemGuid(self, itemGuid):
        self.__itemGuid = itemGuid
    itemGuid = property(__getItemGuid, __setItemGuid)


    def __getRelPath(self):
        r = self.__relPath
        if type(r) is types.UnicodeType:
            r = r.encode("utf-8")
        return r
    def __setRelPath(self, relPath):
        self.__relPath = relPath
    relPath = property(__getRelPath, __setRelPath)


    @property
    def children(self):
        return list(self.__manifestItems)


    @property
    def _children(self):
        return self.__manifestItems


    @property
    def title(self):
        t = self.__title
        if t is None:
            t = self.docTitle
        return t

    def __getTitle(self):
        return self.__title
    def __setTitle(self, title):
        if title is not None:
            title = title.strip()
        if title=="":
            title = None
        self.__title = title
    manifestTitle = property(__getTitle, __setTitle)


    def __getDocTitle(self):
        t = self.__docTitle
        if t=="Untitled" or t=="":
            t = None
        if t is None:
            t = "[Untitled] '%s'" % self.__relPath.split("/")[-1]
        return t
    def __setDocTitle(self, value):
        self.__docTitle = value
    docTitle = property(__getDocTitle, __setDocTitle)


    def __getHidden(self):
        return self.__hidden
    def __setHidden(self, value):
        self.__hidden = bool(value)
    isHidden = property(__getHidden, __setHidden)


    def __getRenditionName(self):
        try:
            return self.__renditionName
        except:
            return ""
    def __setRenditionName(self, value):
        self.__renditionName = value
    renditionName = property(__getRenditionName, __setRenditionName)


    @property
    def hasChildren(self):
        return self.__manifestItems!=[]


    def _insertManifestItem(self, manifestItem, position=99999):
        self.__manifestItems.insert(position, manifestItem)


    def _deleteManifestItem(self, manifestItem):
        if manifestItem in self.__manifestItems:
            for child in manifestItem.children:
                self._insertManifestItem(child)
            self.__manifestItems.remove(manifestItem)


    def _removeManifestItem(self, manifestItem):
        if manifestItem in self.__manifestItems:
            self.__manifestItems.remove(manifestItem)
        return manifestItem


    def hasChild(self, manifestItem):
        return manifestItem in self.__manifestItems


    def hasDesendent(self, manifestItem):
        return self.getParentOf(manifestItem) is not None


    def getParentOf(self, manifestItem):
        parent = None
        if self.hasChild(manifestItem):
            parent = self
        else:
            for child in self.__manifestItems:
                parent = child.getParentOf(manifestItem)
                if parent is not None:
                    break
        return parent


    def __str__(self):
        s = "[ManifestItem] relPath='%s', title='%s', children=%s" % \
                (self.relPath, self.title, len(self.children))
        return s





