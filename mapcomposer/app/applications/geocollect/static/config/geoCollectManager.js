{
   "cookieConsent":true,
   "composerUrl":"",
   "socialUrl":"",
   "start":0,
   "limit":20,
   "msmTimeout":30000,
   "adminUrl": "http://geocollect.geo-solutions.it/opensdi2-manager/",
   "twitter":{
      "via":"geosolutions_it",
      "hashtags":""
   },
   "mediaContent":"./externals/mapmanager/theme/media",
   "ASSET":{
        "delete_icon": "./externals/mapmanager/theme/img/user_delete.png",
        "edit_icon": "./externals/mapmanager/theme/img/user_edit.png"
   },
   "locales":[
      [
         "en",
         "English"
      ],
      [
         "it",
         "Italiano"
      ],
      [
         "fr",
         "Français"
      ],
      [
         "de",
         "Deutsch"
      ],
      [
         "es",
         "Español"
      ]
   ],
   "tools":[{
        "ptype": "mxp_mapmanager",
        "loginManager": "loginTool",
        "actionTarget":null
    },{
        "ptype": "mxp_categoryinitializer",
        "neededCategories": ["GEOCOLLECT", "MAP"]
    },{
        "ptype": "mxp_login",
        "pluginId": "loginTool",
        "actionTarget":{
          "target": "north.tbar",
          "index": 10
        }
    },{ 
        "ptype": "mxp_help",
        "showOnStartup":"true",
        "buttonText": "Info",
        "title":"GeoCollect",
        "text":"Info",
        "iconCls":"information_ic",
        "showOnStartup":true,
        "windowHeight": 320,
        "windowWidth": 637,
        "fileDocURL":"html/splash.html",
        "actionTarget":{
             "target": "north.tbar",
             "index": 15
        }
    },{
        "ptype": "mxp_languageselector",
        "actionTarget":{
          "target": "north.tbar",
          "index": 20
        }
    }],
   "adminTools":[{
        "ptype": "mxp_mapmanager",
        "loginManager": "loginTool",
        "actionTarget": null
    },{
        "ptype": "mxp_templatemanager",
        "loginManager": "loginTool",
		"actionTarget":{
          "target": "north.tbar",
          "index": 0
        }
    },{
        "ptype": "mxp_myaccount",
        "loginManager": "loginTool",
        "actionTarget":{
          "target": "north.tbar",
          "index": 1
        }
    },{
        "ptype": "mxp_usermanager",
        "loginManager": "loginTool",
        "actionTarget":{
          "target": "north.tbar",
          "index": 2
        }
    },{ 
        "ptype": "mxp_geostore_mission_resource_editor",
        "category": "GEOCOLLECT",
        "loginManager": "loginTool",
        "buttonText": "Mission Configuration",
        "actionTarget":{
            "target": "north.tbar",
            "index": 4
        },
        "attributeFields":[{
                    "xtype":"textfield",
                    "readOnly":true,
                     "id":"attribute.templateId",
                     "anchor":'95%',
                     "fieldLabel": "Template Id",
                     "name":"attribute.templateId"
         }
         
         ],
        "resourceEditor":{
            "xtype":"mxp_gc_resource_editor",
            "ref":"/missionResEdit",
            "gcSource":"http://geocollect.geo-solutions.it/geoserver/it.geosolutions/ows?srsName=EPSG:4326&cql_filter=gcid>0&",
            "authParam":"authkey"
        }
    },{ 
        "ptype": "mxp_servicemanager",
        "buttonText": "Photos",
        "notDuplicateOutputs":true,
        "actionTarget":{
            "target": "north.tbar",
            "index": 5
        }
    },{
        "ptype": "mxp_login",
        "pluginId": "loginTool",
        "actionTarget":{
          "target": "north.tbar",
          "index": 10
        }
    },{
        "ptype": "mxp_languageselector",
        "actionTarget":{
          "target": "north.tbar",
          "index": 20
        }
    }],
    "loggedTools":[{
        "ptype": "mxp_mapmanager",
        "loginManager": "loginTool",
        "actionTarget": null
    },{
        "ptype": "mxp_myaccount",
        "loginManager": "loginTool",
        "actionTarget":{
          "target": "north.tbar",
          "index": 1 
        }
    },{ 
        "ptype": "mxp_geostore_mission_resource_editor",
        "category": "GEOCOLLECT",
        "loginManager": "loginTool",
        "buttonText": "Mission Configuration",
        "actionTarget":{
            "target": "north.tbar",
            "index": 2
        },
        "attributeFields":[{
                    "xtype":"textfield",
                    "readOnly":true,
                     "id":"attribute.templateId",
                     "anchor":'95%',
                     "fieldLabel": "Template Id",
                     "name":"attribute.templateId"
         }
         
         ],
        "resourceEditor":{
            "xtype":"mxp_gc_resource_editor",
            "ref":"/missionResEdit",
            "gcSource":"http://geocollect.geo-solutions.it/geoserver/it.geosolutions/ows?srsName=EPSG:4326&cql_filter=gcid>0&",
            "authParam":"authkey"
        }
    },{
        "ptype": "mxp_login",
        "pluginId": "loginTool",
        "actionTarget":{
          "target": "north.tbar",
          "index": 10
        }
    },{
        "ptype": "mxp_languageselector",
        "actionTarget":{
          "target": "north.tbar",
          "index": 20
        }
    }],
   "embedLink": {
		"embeddedTemplateName": "viewer",
		"showDirectURL": true,
        "showQRCode":false,
        "qrCodeSize":128,
        "showMapStoreMobileSource":false,
        "appDownloadUrl":"http://build.geo-solutions.it/mapstoremobile/downloads/releases/1.0.0/developement/GeoCollect.apk",
        "googlePlayLink":"https://play.google.com/apps/testing/it.geosolutions.geocollect.android.app"
	},
    "availableMapButtons":[
        "editinfo",
        "deletemap",
        "viewmap"
    ]

}