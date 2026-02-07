import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  input,
  OnDestroy,
  signal,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { SpaceService } from '../../services/space.service';
import ForceGraph3D from '3d-force-graph';
import { animate, stagger } from 'animejs';

interface ReasoningNode {
  id: string;
  label: string;
  type: 'CONCLUSION' | 'EVIDENCE' | 'WHAT_IF' | 'FINAL_CONCLUSION';
  turn: number;
  content: string;
}

@Component({
  selector: 'app-reasoning-graph',
  imports: [CommonModule, ButtonModule],
  templateUrl: './reasoning-graph.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReasoningGraphComponent implements AfterViewInit, OnDestroy {
  spaceId = input.required<string>();
  showHeader = input(true);
  @ViewChild('graphContainer') graphContainer!: ElementRef;
  @ViewChild('controlsPanel') controlsPanel!: ElementRef;

  private graph: any;
  private resizeHandler?: () => void;
  graphReady = signal(false);
  refreshToken = signal(0);
  loading = signal(false);

  constructor(private readonly spaceService: SpaceService) {
    effect((onCleanup) => {
      const spaceId = this.spaceId();
      const ready = this.graphReady();
      this.refreshToken();
      if (!spaceId || !ready) return;

      this.loading.set(true);
      const sub = this.spaceService.getTree(spaceId).subscribe({
        next: (response) => {
          this.buildForceGraph(response);
          this.loading.set(false);
        },
        error: (error) => {
          console.error('Failed to load graph:', error);
          this.loading.set(false);
        },
      });

      onCleanup(() => sub.unsubscribe());
    });
  }

  ngAfterViewInit() {
    this.animateControlsPanel();
    this.initGraph();
  }

  ngOnDestroy() {
    if (this.graph) {
      this.graph._destructor?.();
    }
    if (this.resizeHandler) {
      window.removeEventListener('resize', this.resizeHandler);
    }
  }

  private animateControlsPanel() {
    if (this.controlsPanel) {
      animate(this.controlsPanel.nativeElement, {
        opacity: [0, 1],
        translateY: [-20, 0],
        duration: 600,
        easing: 'easeOutCubic',
      });
    }
  }

  private initGraph() {
    this.graph = (ForceGraph3D as any)()(this.graphContainer.nativeElement);

    // Configure graph appearance with dark theme
    this.graph
      .width(this.graphContainer.nativeElement.clientWidth)
      .height(this.graphContainer.nativeElement.clientHeight)
      .backgroundColor('#0f172a')
      .nodeOpacity(0.95)
      .nodeResolution(8)
      .linkOpacity(0.6)
      .linkWidth(2)
      .linkDirectionalArrowLength(6)
      .linkDirectionalArrowRelPos(1)
      .enableNodeDrag(true)
      .enableNavigationInteraction(true)
      .onNodeHover((node: any) => {
        this.graphContainer.nativeElement.style.cursor = node ? 'pointer' : null;
      })
      .onNodeClick((node: any) => {
        console.log('Node clicked:', node);
      });

    // Auto-fit on window resize
    this.resizeHandler = () => {
      this.graph
        .width(this.graphContainer.nativeElement.clientWidth)
        .height(this.graphContainer.nativeElement.clientHeight);
    };
    window.addEventListener('resize', this.resizeHandler);
    this.graphReady.set(true);
  }

  loadGraphData() {
    this.refreshToken.update((value) => value + 1);
  }

  private buildForceGraph(data: any) {
    const treeNodes = data.tree || [];
    const nodes: any[] = [];
    const links: any[] = [];
    const nodeMap = new Map<string, any>();

    // Create nodes
    treeNodes.forEach((node: any) => {
      const nodeObj = {
        id: node.id,
        name: this.truncate(node.content || node.label || '', 40),
        type: node.type,
        color: this.getNodeColor(node.type),
        size: this.getNodeSize(node.type),
        val: this.getNodeSize(node.type),
      };
      nodes.push(nodeObj);
      nodeMap.set(node.id, nodeObj);
    });

    // Create links
    treeNodes.forEach((node: any) => {
      // Evidence links
      if (node.evidences && Array.isArray(node.evidences)) {
        node.evidences.forEach((evidence: any) => {
          const targetId = typeof evidence === 'string' ? evidence : evidence.id;
          if (nodeMap.has(targetId)) {
            links.push({
              source: node.id,
              target: targetId,
              label: 'EVIDENCE',
              color: '#6B7280',
            });
          }
        });
      }

      // Influenced conclusions
      if (node.influencedConclusions && Array.isArray(node.influencedConclusions)) {
        node.influencedConclusions.forEach((conc: any) => {
          const targetId = typeof conc === 'string' ? conc : conc.id;
          if (nodeMap.has(targetId)) {
            links.push({
              source: node.id,
              target: targetId,
              label: 'INFLUENCES',
              color: '#F59E0B',
            });
          }
        });
      }

      // What-if scenarios
      if (node.whatIf) {
        const whatIfId = typeof node.whatIf === 'string' ? node.whatIf : node.whatIf.id;
        if (nodeMap.has(whatIfId)) {
          links.push({
            source: whatIfId,
            target: node.id,
            label: 'GENERATES',
            color: '#F59E0B',
          });
        }
      }
    });

    // Update graph with data
    this.graph.graphData({ nodes, links });

    // Styling
    this.graph
      .nodeVal((d: any) => d.val)
      .nodeColor((d: any) => d.color)
      .nodeLabel((d: any) => `${d.name}\n(${d.type})`)
      .linkColor((d: any) => d.color)
      .linkLabel((d: any) => d.label)
      .d3Force('charge')
      .strength(-300);

    // Auto-center view with animation
    setTimeout(() => {
      this.graph.zoomToFit(400);
      this.animateNodeEntrance();
    }, 300);
  }

  private animateNodeEntrance() {
    // Animate node entrance with stagger
    animate('.node-sphere', {
      opacity: [0, 1],
      duration: 1000,
      easing: 'easeOutCubic',
      delay: stagger(50),
    });
  }

  private getNodeColor(type: string): string {
    switch (type) {
      case 'CONCLUSION':
        return '#3B82F6';
      case 'EVIDENCE':
        return '#6B7280';
      case 'WHAT_IF':
        return '#F59E0B';
      case 'FINAL_CONCLUSION':
        return '#DC2626';
      default:
        return '#8B5CF6';
    }
  }

  private getNodeSize(type: string): number {
    switch (type) {
      case 'FINAL_CONCLUSION':
        return 15;
      case 'CONCLUSION':
        return 12;
      case 'WHAT_IF':
        return 10;
      case 'EVIDENCE':
        return 8;
      default:
        return 10;
    }
  }

  private truncate(text: string, maxLength: number): string {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }
}
