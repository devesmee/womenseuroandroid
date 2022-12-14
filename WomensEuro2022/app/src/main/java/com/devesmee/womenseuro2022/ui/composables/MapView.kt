package com.devesmee.womenseuro2022.ui.composables

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devesmee.womenseuro2022.R
import com.devesmee.womenseuro2022.activities.StadiumActivity
import com.devesmee.womenseuro2022.models.Stadium
import com.google.gson.Gson
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapView(
    stadiums: List<Stadium>,
    modifier: Modifier = Modifier,
    onLoad: ((map: MapView) -> Unit)? = null
) {
    val navController = rememberNavController()
    val mapViewState = rememberMapViewWithLifecycle(
        stadiums = stadiums
    )

    NavHost(navController = navController, startDestination = "mapView") {
        composable("mapView") {
            AndroidView(
                {
                    mapViewState
                },
                modifier
            ) { mapView -> onLoad?.invoke(mapView)
            }
        }
        composable(
            "stadiumDetailView/{stadiumJSON}",
            arguments = listOf(navArgument("stadiumJSON") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("stadiumJSON")?.let { stadiumJSON ->
                val context = LocalContext.current
                val intent = Intent(context, StadiumActivity::class.java)
                intent.putExtra("stadium", stadiumJSON)
                context.startActivity(intent)
            }
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(
    stadiums: List<Stadium>
): MapView {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
            controller.setZoom(8.5)
            val startPoint = GeoPoint(53.0, -1.0)
            controller.setCenter(startPoint)
        }
    }

    for (stadium in stadiums) {
        val marker = Marker(mapView)
        marker.icon = resizeLogo(logo = AppCompatResources.getDrawable(context, R.drawable.logo))
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.position = GeoPoint(stadium.latitude, stadium.longitude)
        marker.infoWindow = null
        marker.setOnMarkerClickListener { _, _ ->
            val stadiumJSON = Gson().toJson(stadium)
            // navigate to stadium detail view
            navigateToStadiums(stadiumJSON, context)
            true
        }

        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    // Makes MapView follow the lifecycle of this composable
    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
    }

@Composable
fun resizeLogo(logo: Drawable?): Drawable {
    val logoRoundedDrawable = RoundedBitmapDrawableFactory.create(LocalContext.current.resources, logo?.toBitmap(100, 100))
    logoRoundedDrawable.isCircular = true

    val logoRoundedBitmap = logoRoundedDrawable.toBitmap(250,250)

    return BitmapDrawable(logoRoundedBitmap)
}

private fun navigateToStadiums(stadiumJSON: String, context: Context) {
    val intent = Intent(context, StadiumActivity::class.java)
    intent.putExtra("stadium", stadiumJSON)
    context.startActivity(intent)
}
